package cn.x5456.rs.server.controller;

import cn.hutool.core.util.IdUtil;
import cn.x5456.rs.def.IResourceStorage;
import cn.x5456.rs.entity.ResourceInfo;
import cn.x5456.infrastructure.util.DataBufferUtilsExt;
import cn.x5456.infrastructure.util.FileDownloadUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.*;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static cn.x5456.rs.constant.DataBufferConstant.DEFAULT_CHUNK_SIZE;

/**
 * @author yujx
 * @date 2021/05/08 11:22
 */
@Api(tags = "文件存储服务")
@RestController
public class RSController {

    @Autowired
    private IResourceStorage resourceStorage;

    @Autowired
    private DataBufferFactory dataBufferFactory;

    @ApiOperation("上传小文件")
    @PostMapping(value = "/v1/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResourceInfo> requestBodyFlux(@RequestPart("file") FilePart filePart) {
        String filename = filePart.filename();
        String path = IdUtil.objectId();
        Flux<DataBuffer> content = filePart.content();

        return resourceStorage.uploadFile(content, filename, path);
    }

    // TODO: 2021/5/8 好像是不写 content-type 就直接下载，写了才会预览 -> 当我没说，不知道，到时候学习下
    /*
    curl -I -H 'Range: bytes=1-7' 'http://127.0.0.1:8080/v1/download/6096804ecdb2eff210599827'
     */
    @ApiOperation("下载/预览文件")
    @GetMapping("/v1/download/{path}")
    public Mono<Void> download(@PathVariable String path,
                               ServerHttpRequest request, ServerHttpResponse response) {
        return resourceStorage.downloadFile(path)
                .flatMap(pair -> {
                    String fileName = pair.getKey();
                    String localFilePath = pair.getValue();

                    // 获取 range
                    HttpHeaders requestHeaders = request.getHeaders();
                    List<HttpRange> rangeList = requestHeaders.getRange();
                    if (rangeList.size() > 1) {
                        return Mono.error(new RuntimeException("请求头中 range 的数量不合法！"));
                    }

                    // 设置文件名的响应头
                    HttpHeaders responseHeaders = response.getHeaders();
                    ContentDisposition disposition = ContentDisposition.builder("attachment")
                            .filename(fileName, StandardCharsets.UTF_8)
                            .build();
                    responseHeaders.setContentDisposition(disposition);

                    // 如果没有设置 range 则全部返回
                    File file = new File(localFilePath);
                    long length = file.length();
                    if (rangeList.size() == 0) {
                        if (response instanceof ZeroCopyHttpOutputMessage) {
                            return ((ZeroCopyHttpOutputMessage) response).writeWith(file, 0, length);
                        }
                        Flux<DataBuffer> bufferFlux = DataBufferUtils.read(
                                new FileSystemResource(localFilePath), dataBufferFactory, DEFAULT_CHUNK_SIZE);
                        return response.writeWith(bufferFlux);
                    }

                    // 获取请求头中的 range
                    HttpRange range = rangeList.get(0);

                    // 获取 range 的范围
                    long rangeStart = range.getRangeStart(length);
                    long rangeEnd = range.getRangeEnd(length);

                    // 写响应头
                    responseHeaders.add(HttpHeaders.ACCEPT_RANGES, "bytes");
                    responseHeaders.add(HttpHeaders.CONTENT_RANGE, FileDownloadUtil.createContentRange(range, length));
                    responseHeaders.setContentLength(rangeEnd - rangeStart);
                    responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);

                    // 设置响应状态码
                    response.setStatusCode(HttpStatus.PARTIAL_CONTENT);

                    if (response instanceof ZeroCopyHttpOutputMessage) {
                        return ((ZeroCopyHttpOutputMessage) response).writeWith(file, rangeStart, rangeEnd);
                    }
                    Flux<DataBuffer> bufferFlux = DataBufferUtilsExt.read(
                            new FileSystemResource(localFilePath), rangeStart, rangeEnd, dataBufferFactory, DEFAULT_CHUNK_SIZE);
                    return response.writeWith(bufferFlux);
                });
    }

}
