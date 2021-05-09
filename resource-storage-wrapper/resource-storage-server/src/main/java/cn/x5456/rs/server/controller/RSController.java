package cn.x5456.rs.server.controller;

import cn.hutool.core.util.IdUtil;
import cn.x5456.infrastructure.util.DataBufferUtilsExt;
import cn.x5456.infrastructure.util.FileDownloadUtil;
import cn.x5456.rs.def.IResourceStorage;
import cn.x5456.rs.entity.ResourceInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
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
    curl -i -H 'Range: bytes=1-7' 'http://127.0.0.1:8080/v1/download/6096804ecdb2eff210599827'
    curl -i -H 'Range: bytes=7-' 'http://127.0.0.1:8080/v1/download/6096804ecdb2eff210599827'
    curl -i -H 'Range: bytes=0-2, 2-5' 'http://127.0.0.1:8080/v1/download/6096804ecdb2eff210599827'

    https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Range_requests

    GetMapping 也可以接收 HEAD 请求的，所以需要在响应头中加上 Accept-Ranges: bytes 和 Content-Length: 146515
     */
    @Deprecated
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
                        response.setStatusCode(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE); // 这个未生效
                        return Mono.error(new RuntimeException("请求头中 range 的数量不合法！"));
                    }

                    // 设置文件名的响应头
                    HttpHeaders responseHeaders = response.getHeaders();
                    ContentDisposition disposition = ContentDisposition.builder("attachment")
                            .filename(fileName, StandardCharsets.UTF_8)
                            .build();
                    responseHeaders.setContentDisposition(disposition);
                    responseHeaders.add(HttpHeaders.ACCEPT_RANGES, "bytes");

                    // 如果没有设置 range 则全部返回
                    File file = new File(localFilePath);
                    long length = file.length();
                    if (rangeList.size() == 0) {
                        responseHeaders.setContentLength(length);
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

    /*
    参考资料：
    https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Range_requests
    https://juejin.cn/post/6844904061645357063

    Spring 实现：ResourceHttpMessageWriter  canWrite() + write() 方法

    >> 测试：

    HEAD

    curl -I 'http://127.0.0.1:8080/v2/download/6096804ecdb2eff210599827'
    HTTP/1.1 200 OK
    transfer-encoding: chunked
    Accept-Ranges: bytes
    Content-Type: text/plain
    Content-Length: 17

    GET1

    curl -i 'http://127.0.0.1:8080/v2/download/6096804ecdb2eff210599827'
    HTTP/1.1 200 OK
    Accept-Ranges: bytes
    Content-Type: text/plain
    Content-Length: 17

    1234567890abcdefg%

    GET2

    curl -i -H 'Range: bytes=1-7' 'http://127.0.0.1:8080/v2/download/6096804ecdb2eff210599827'
    curl -i -H 'Range: bytes=0-16' 'http://127.0.0.1:8080/v2/download/6096804ecdb2eff210599827' -> 这个代表全部数据，他是 [0, 16] 而不是 [0, 16)
    HTTP/1.1 206 Partial Content
    Accept-Ranges: bytes
    Content-Type: text/plain
    Content-Range: bytes 1-7/17
    Content-Length: 7

    2345678

    GET3

    curl -i -H 'Range: bytes=0-2, 2-5' 'http://127.0.0.1:8080/v2/download/6096804ecdb2eff210599827'
    HTTP/1.1 206 Partial Content
    transfer-encoding: chunked
    Accept-Ranges: bytes
    Content-Type: multipart/byteranges;boundary=U4LNbnq5mkJD6nhreZilpGyxRQIi6n1xZZrKgmG


    --U4LNbnq5mkJD6nhreZilpGyxRQIi6n1xZZrKgmG
    Content-Type: text/plain
    Content-Range: bytes 0-2/17

    123
    --U4LNbnq5mkJD6nhreZilpGyxRQIi6n1xZZrKgmG
    Content-Type: text/plain
    Content-Range: bytes 2-5/17

    3456
    --U4LNbnq5mkJD6nhreZilpGyxRQIi6n1xZZrKgmG--

    GET4

    curl -i -H 'Range: bytes=1-0' 'http://127.0.0.1:8080/v2/download/6096804ecdb2eff210599827'
    HTTP/1.1 416 Requested Range Not Satisfiable
    Content-Type: application/octet-stream
    Accept-Ranges: bytes
    content-length: 0
     */
    @ApiOperation("下载文件v2（支持分段下载 - Range）")
    @GetMapping("/v2/download/{path}")
    public Mono<Resource> downloadV2(@PathVariable String path, ServerHttpResponse response) {
        return resourceStorage.downloadFile(path)
                .flatMap(pair -> {
                    String fileName = pair.getKey();
                    String localFilePath = pair.getValue();

                    // 设置文件名的响应头
                    HttpHeaders responseHeaders = response.getHeaders();
                    ContentDisposition disposition = ContentDisposition.builder("attachment")
                            .filename(fileName, StandardCharsets.UTF_8)
                            .build();
                    responseHeaders.setContentDisposition(disposition);

                    // TODO: 2021/5/9 应该是可以通过这个值来实现浏览器预览功能的 -> 好像不行，可能是因为我的文件后缀不是 pdf 这样的
                    // 设置 Content-Type，不设置的话默认为 text/html 会直接在页面"打开"
                    responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                    return Mono.just(new FileSystemResource(localFilePath));
                });
    }

}
