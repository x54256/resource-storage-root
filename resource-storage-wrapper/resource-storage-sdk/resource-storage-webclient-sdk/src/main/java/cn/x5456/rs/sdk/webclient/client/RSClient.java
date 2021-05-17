package cn.x5456.rs.sdk.webclient.client;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Pair;
import cn.x5456.rs.common.ResponseData;
import cn.x5456.rs.def.UploadProgress;
import cn.x5456.rs.entity.ResourceInfo;
import cn.x5456.rs.sdk.webclient.lb.ConsistentHashLoadBalancerFactory;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author yujx
 * @date 2021/05/17 09:36
 */
@Component
public class RSClient {

    // TODO: 2021/5/17 不知道为啥用 @Value 注解注入不行
    private String rsUrl = "http://rs-server/rest/rs";

    @Autowired
    private ConsistentHashLoadBalancerFactory lbFunction;

    private WebClient client;

    /*
      参考：https://segmentfault.com/a/1190000021133071
     */
    @PostConstruct
    public void init() {
        HttpClient httpClient = HttpClient.create()
                .tcpConfiguration(tcpClient -> tcpClient.doOnConnected(connection -> {
                    //读写超时设置
                    connection.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS)).addHandlerLast(new WriteTimeoutHandler(10));
                })
                        //连接超时设置
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                        .option(ChannelOption.TCP_NODELAY, true));

        this.client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> {
                    // 设置最大内存占用为 2M
                    configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024);
                    configurer.customCodecs().register(new Jackson2JsonDecoder());
                    configurer.customCodecs().register(new Jackson2JsonEncoder());
                })
                .filter(lbFunction)
                .baseUrl(rsUrl)
                .build();
    }


    public Mono<ResourceInfo> uploadFile(File file) {

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new FileSystemResource(file));
        MultiValueMap<String, HttpEntity<?>> parts = builder.build();

        return client.post()
                .uri("/v1/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(parts)
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError,
                        resp -> resp.bodyToMono(ResponseData.class).map(responseData -> new RuntimeException(responseData.getMessage())))
                .bodyToMono(ResponseData.class)
                .map(x -> BeanUtil.toBean(x.getData(), ResourceInfoImpl.class));
    }

    public Mono<ClientResponse> downloadV2(String path, HttpRange... httpRanges) {
        return this.getFileHash(path)
                .flatMap(fileHash -> {
                    WebClient.RequestHeadersSpec<?> requestHeadersSpec = client.get()
                            .uri("/v2/files/{path}", path);
                    if (httpRanges.length > 0) {
                        requestHeadersSpec = requestHeadersSpec.header(HttpHeaders.RANGE, HttpRange.toString(Arrays.asList(httpRanges)));
                    }

                    // 使用 hash 环算法负载到文件服务器
                    return requestHeadersSpec
                            .attribute(ConsistentHashLoadBalancerFactory.FILE_HASH, fileHash)
                            .exchange();
                });
    }

    public Mono<Boolean> delete(String path) {
        return client.delete()
                .uri("/v1/files/{path}", path)
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError,
                        resp -> resp.bodyToMono(ResponseData.class).map(responseData -> new RuntimeException(responseData.getMessage())))
                .bodyToMono(ResponseData.class)
                .map(x -> (Boolean) x.getData());
    }

    // ======= 大文件上传 api

    public Mono<Boolean> isExist(String fileHash) {
        return client.get()
                .uri("/v1/files/big/secondPass/{fileHash}", fileHash)
                .retrieve()
                .bodyToMono(ResponseData.class)
                .map(x -> (Boolean) x.getData());
    }

    public Mono<ResourceInfo> secondPass(String fileHash, String fileName) {
        return client.post()
                .uri("/v1/files/big/secondPass/{fileHash}?fileName={fileName}", fileHash, fileName)
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError,
                        resp -> resp.bodyToMono(ResponseData.class).map(responseData -> new RuntimeException(responseData.getMessage())))
                .bodyToMono(ResponseData.class)
                .map(x -> BeanUtil.toBean(x.getData(), ResourceInfoImpl.class));
    }

    public Mono<Boolean> uploadFileChunk(String fileHash, int chunk, File file) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new FileSystemResource(file));
        MultiValueMap<String, HttpEntity<?>> parts = builder.build();

        return client.post()
                .uri("/v1/files/big/{fileHash}/{chunk}", fileHash, chunk)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(parts)
                // 使用 hash 环算法负载到文件服务器
                .attribute(ConsistentHashLoadBalancerFactory.FILE_HASH, fileHash)
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError,
                        resp -> resp.bodyToMono(ResponseData.class).map(responseData -> new RuntimeException(responseData.getMessage())))
                .bodyToMono(ResponseData.class)
                .map(x -> (Boolean) x.getData());
    }

    public Flux<Pair<Integer, UploadProgress>> uploadProgress(String fileHash) {
        return client.get()
                .uri("/v1/files/big/{fileHash}", fileHash)
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError,
                        resp -> resp.bodyToMono(ResponseData.class).map(responseData -> new RuntimeException(responseData.getMessage())))
                .bodyToMono(ResponseData.class)
                .flux()
                .flatMap(responseData -> Flux.fromIterable((List<Pair<Integer, UploadProgress>>) responseData.getData()));
    }

    public Mono<ResourceInfo> uploadCompleted(String fileHash, String fileName, int totalNumberOfChunks) {
        return client.post()
                .uri("/v1/files/big/{fileHash}?fileName={fileName}&totalNumberOfChunks={totalNumberOfChunks}", fileHash, fileName, totalNumberOfChunks)
                // 使用 hash 环算法负载到文件服务器
                .attribute(ConsistentHashLoadBalancerFactory.FILE_HASH, fileHash)
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError,
                        resp -> resp.bodyToMono(ResponseData.class).map(responseData -> new RuntimeException(responseData.getMessage())))
                .bodyToMono(ResponseData.class)
                .map(x -> BeanUtil.toBean(x.getData(), ResourceInfoImpl.class));
    }

    public Mono<Boolean> uploadError(String fileHash) {
        return client.delete()
                .uri("/v1/files/big/{fileHash}", fileHash)
                // 使用 hash 环算法负载到文件服务器
                .attribute(ConsistentHashLoadBalancerFactory.FILE_HASH, fileHash)
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError,
                        resp -> resp.bodyToMono(ResponseData.class).map(responseData -> new RuntimeException(responseData.getMessage())))
                .bodyToMono(ResponseData.class)
                .map(x -> (Boolean) x.getData());
    }

    // ======= 其它 api

    public Mono<String> getFileHash(String path) {
        return client.get()
                .uri("/v1/files/hash/{path}", path)
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError,
                        resp -> resp.bodyToMono(ResponseData.class).map(responseData -> new RuntimeException(responseData.getMessage())))
                .bodyToMono(ResponseData.class)
                .map(x -> (String) x.getData());
    }

    public Mono<Object> getAttachment(String path, String key) {

        return this.getFileHash(path)
                .map(fileHash -> client.get()
                        .uri("/v1/files/attachment/{path}/{key}", path, key)
                        // 使用 hash 环算法负载到文件服务器
                        .attribute(ConsistentHashLoadBalancerFactory.FILE_HASH, fileHash)
                        .retrieve()
                        .onStatus(HttpStatus::is5xxServerError,
                                resp -> resp.bodyToMono(ResponseData.class).map(responseData -> new RuntimeException(responseData.getMessage())))
                        .bodyToMono(ResponseData.class)
                        .map(ResponseData::getData));
    }

    @Data
    private static class ResourceInfoImpl implements ResourceInfo {

        private String id;

        // 文件名
        private String fileName;

        // 文件 hash 值
        private String fileHash;

        // 元数据 id
        @Deprecated
        private String metadataId;
    }
}
