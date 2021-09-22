package cn.x5456.rs.server.controller.preview;

import cn.x5456.rs.entity.ResourceInfo;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;

/**
 * @author yujx
 * @date 2021/09/17 15:09
 */
public interface FilePreviewHandler {

    boolean supports(ResourceInfo resourceInfo);

    Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response, ResourceInfo resourceInfo, String localFilePath, String platform, String mode);

    default Mono<Object> getDocModel(ServerHttpRequest request, ResourceInfo resourceInfo, String platform, String mode) {
        throw new UnsupportedOperationException();
    }
}
