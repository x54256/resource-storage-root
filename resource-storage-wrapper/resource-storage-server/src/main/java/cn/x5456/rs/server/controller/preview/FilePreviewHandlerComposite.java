package cn.x5456.rs.server.controller.preview;

import cn.x5456.rs.entity.ResourceInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * @author yujx
 * @date 2021/09/17 15:23
 */
@Component
public class FilePreviewHandlerComposite {

    @Autowired
    private List<FilePreviewHandler> handlers;

    public boolean supports(ResourceInfo resourceInfo) {
        return this.getHandler(resourceInfo) != null;
    }

    private FilePreviewHandler getHandler(ResourceInfo resourceInfo) {
        for (FilePreviewHandler handler : handlers) {
            if (handler.supports(resourceInfo)) {
                return handler;
            }
        }
        return null;
    }

    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response, ResourceInfo resourceInfo, String localFilePath, String platform, String mode) {
        return Optional.ofNullable(this.getHandler(resourceInfo))
                .orElseThrow(() -> new RuntimeException("当前文件类型暂不支持预览！"))
                .handle(request, response, resourceInfo, localFilePath, platform, mode);
    }

    public Mono<Object> getDocModel(ServerHttpRequest request, ResourceInfo resourceInfo, String platform, String mode) {
        return Optional.ofNullable(this.getHandler(resourceInfo))
                .orElseThrow(() -> new RuntimeException("当前文件类型暂不支持预览！"))
                .getDocModel(request, resourceInfo, platform, mode);
    }
}
