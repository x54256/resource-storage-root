package cn.x5456.rs.server.controller;

import cn.hutool.core.util.IdUtil;
import cn.x5456.rs.def.IResourceStorage;
import cn.x5456.rs.entity.ResourceInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author yujx
 * @date 2021/05/08 11:22
 */
@Api(tags = "文件存储服务")
@RestController
public class RSController {

    @Autowired
    private IResourceStorage resourceStorage;

    /*
    Unable to create the inputStream. /var/folders/28/1tyh6prj3xg6xcdx_3qlkwr80000gn/T/nio-file-upload/nio-body-1-91fd3971-e0e1-4d62-beae-28f5faa1da32.tmp
	at org.synchronoss.cloud.nio.stream.storage.FileStreamStorage.newFileInputStream

    https://github.com/spring-projects/spring-framework/issues/22298

    记录.txt
     */
    @Deprecated
    @ApiOperation("上传小文件")
    @PostMapping(value = "/v1/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResourceInfo> requestBodyFlux(@RequestPart("file") FilePart filePart) {
        String filename = filePart.filename();
        String path = IdUtil.objectId();
        Flux<DataBuffer> content = filePart.content();

        return resourceStorage.uploadFile(content, filename, path);
    }

    // TODO: 2021/5/8 好像是不写 content-type 就直接下载，写了才会预览 -> 当我没说，不知道，到时候学习下
    @ApiOperation("下载/预览文件")
    @GetMapping("/v1/download/{path}")
    public Mono<Void> download(@PathVariable String path, ServerHttpResponse response) {
        return resourceStorage.downloadFileDataBuffer(path)
                .flatMap(pair -> {
                    response.getHeaders().set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + pair.getKey());
//                    response.getHeaders().setContentType(MediaType.APPLICATION_PDF);
                    return response.writeWith(pair.getValue());
                });
    }

}
