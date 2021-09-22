package cn.x5456.rs.server.controller.preview;

import cn.hutool.core.util.StrUtil;
import cn.x5456.rs.entity.ResourceInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static cn.x5456.rs.constant.DataBufferConstant.DEFAULT_CHUNK_SIZE;

/**
 * @author yujx
 * @date 2021/09/17 15:19
 */
@Component
public class SimpleHandler implements FilePreviewHandler {

    @Autowired
    private DataBufferFactory dataBufferFactory;

    @Override
    public boolean supports(ResourceInfo resourceInfo) {
        return StrUtil.isNotBlank(resourceInfo.getMimeType());
    }

    /**
     * 使用浏览器自带预览
     */
    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response, ResourceInfo resourceInfo, String localFilePath, String platform, String mode) {
        response.getHeaders().setContentType(MediaType.parseMediaType(resourceInfo.getMimeType()));
        Flux<DataBuffer> dataBufferFlux = DataBufferUtils.read(new FileSystemResource(localFilePath), dataBufferFactory, DEFAULT_CHUNK_SIZE);
        return response.writeAndFlushWith(Mono.just(dataBufferFlux));
    }
}
