package cn.x5456.rs.server.controller;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.IdUtil;
import cn.x5456.rs.common.UnWrapper;
import cn.x5456.rs.def.IResourceStorage;
import cn.x5456.rs.def.UploadProgress;
import cn.x5456.rs.entity.ResourceInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * @author yujx
 * @date 2021/05/08 11:22
 */
@Api(tags = "文件存储服务")
@RestController
@RequestMapping("/rest/rs")
public class RSController {

    @Autowired
    private IResourceStorage resourceStorage;

    @ApiOperation("上传小文件")
    @PostMapping(value = "/v1/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResourceInfo> uploadFile(@RequestPart("file") FilePart filePart) {
        String filename = filePart.filename();
        String path = IdUtil.objectId();
        Flux<DataBuffer> content = filePart.content();

        return resourceStorage.uploadFile(content, filename, path);
    }


    /*
    暂时放弃预览功能。
    2021/5/8 好像是不写 content-type 就直接下载，写了才会预览 -> 当我没说，不知道，到时候学习下
    2021/5/9 文件预览记得过滤掉 jsp 这种东西，防止那几个攻击
     */


    /*
    参考资料：
    https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Range_requests
    https://juejin.cn/post/6844904061645357063

    Spring 实现：ResourceHttpMessageWriter  canWrite() + write() 方法

    注： 之前 v1 版本写的是错误的
        1. postman 调用不通
        2. v1 版本 range 返回的是 [) 结构，正确的是 []

    >> 测试：

    HEAD

    curl -I 'http://127.0.0.1:8080/v2/files/6096804ecdb2eff210599827'
    HTTP/1.1 200 OK
    transfer-encoding: chunked
    Accept-Ranges: bytes
    Content-Type: text/plain
    Content-Length: 17

    GET1

    curl -i 'http://127.0.0.1:8080/v2/files/6096804ecdb2eff210599827'
    HTTP/1.1 200 OK
    Accept-Ranges: bytes
    Content-Type: text/plain
    Content-Length: 17

    1234567890abcdefg%

    GET2

    curl -i -H 'Range: bytes=1-7' 'http://127.0.0.1:8080/v2/files/6096804ecdb2eff210599827'
    curl -i -H 'Range: bytes=0-16' 'http://127.0.0.1:8080/v2/files/6096804ecdb2eff210599827' -> 这个代表全部数据，他是 [0, 16] 而不是 [0, 16)
    HTTP/1.1 206 Partial Content
    Accept-Ranges: bytes
    Content-Type: text/plain
    Content-Range: bytes 1-7/17
    Content-Length: 7

    2345678

    GET3

    curl -i -H 'Range: bytes=0-2, 2-5' 'http://127.0.0.1:8080/v2/files/6096804ecdb2eff210599827'
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

    curl -i -H 'Range: bytes=1-0' 'http://127.0.0.1:8080/v2/files/6096804ecdb2eff210599827'
    HTTP/1.1 416 Requested Range Not Satisfiable
    Content-Type: application/octet-stream
    Accept-Ranges: bytes
    content-length: 0
     */
    @UnWrapper
    @ApiOperation("下载文件v2（支持分段下载 - Range）")
    @GetMapping("/v2/files/{path}")
    public Mono<Resource> downloadV2(@ApiParam("资源文件的唯一标识") @PathVariable String path, ServerHttpResponse response) {
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
                    return Mono.just(new FileSystemResource(localFilePath));
                });
    }

    /*
    文件预览测试结果：
    1. 加 MediaType 的请求头 Accept "可能可以"，但是无法测试
    2. responseHeaders 不能 setContentDisposition(disposition);
    3. 只要 setContentDisposition 了，那么一定是下载，不用设置响应 content-type

    curl -i -H 'Accept: image/png' 'http://127.0.0.1:8080/rest/rs/v2/files/preview/6098c2c0ee92ec96a351eb7d' image/png
     */

    @ApiOperation("删除文件")
    @DeleteMapping("/v1/files/{path}")
    public Mono<Boolean> delete(@ApiParam("资源文件的唯一标识") @PathVariable String path) {
        return resourceStorage.deleteFile(path);
    }

    @ApiOperation("大文件上传 - 检查文件是否已经上传过了")
    @GetMapping("/v1/files/big/secondPass/{fileHash}")
    public Mono<Boolean> isExist(@ApiParam("文件 hash 值，使用 sha256 计算") @PathVariable String fileHash) {
        return resourceStorage.getBigFileUploader().isExist(fileHash);
    }

    @ApiOperation("大文件上传 - 秒传")
    @PostMapping("/v1/files/big/secondPass/{fileHash}")
    public Mono<ResourceInfo> secondPass(@ApiParam("文件 hash 值，使用 sha256 计算") @PathVariable String fileHash,
                                         @ApiParam("文件名") @RequestParam String fileName) {
        String path = IdUtil.objectId();
        return resourceStorage.getBigFileUploader().secondPass(fileHash, fileName, path);
    }

    @ApiOperation("大文件上传 - 上传每一片")
    @PostMapping("/v1/files/big/{fileHash}/{chunk}")
    public Mono<Boolean> uploadFileChunk(@ApiParam("文件 hash 值，使用 sha256 计算") @PathVariable String fileHash,
                                         @ApiParam("当前文件的第几块") @PathVariable int chunk,
                                         @RequestPart("file") FilePart filePart) {
        return resourceStorage.getBigFileUploader().uploadFileChunk(fileHash, chunk, filePart.content());
    }

    @ApiOperation("大文件上传 - 获取上传进度")
    @GetMapping("/v1/files/big/{fileHash}")
    public Flux<Pair<Integer, UploadProgress>> uploadProgress(@ApiParam("文件 hash 值，使用 sha256 计算") @PathVariable String fileHash) {
        return resourceStorage.getBigFileUploader().uploadProgress(fileHash);
    }

    @ApiOperation("大文件上传 - 全部上传成功，执行“合并”操作")
    @PostMapping("/v1/files/big/{fileHash}")
    public Mono<ResourceInfo> uploadCompleted(@ApiParam("文件 hash 值，使用 sha256 计算") @PathVariable String fileHash,
                                              @ApiParam("文件名") @RequestParam String fileName,
                                              @ApiParam("当前文件一共分成了多少块") @RequestParam int totalNumberOfChunks) {
        String path = IdUtil.objectId();
        return resourceStorage.getBigFileUploader().uploadCompleted(fileHash, fileName, totalNumberOfChunks, path);
    }

    @ApiOperation("大文件上传 - 上传失败，执行清理操作")
    @DeleteMapping("/v1/files/big/{fileHash}")
    public Mono<Boolean> uploadError(@ApiParam("文件 hash 值，使用 sha256 计算") @PathVariable String fileHash) {
        return resourceStorage.getBigFileUploader().uploadError(fileHash);
    }

    @ApiOperation("获取文件附件信息")
    @GetMapping("/v1/files/attachment/{path}/{key}")
    public Mono<Object> getAttachment(@PathVariable @ApiParam(value = "文件存储标识") String path,
                                      @PathVariable @ApiParam(value = "附件类型") String key) {
        return resourceStorage.getAttachment(path, key, Object.class);
    }

    @ApiOperation("根据path查询hash值")
    @GetMapping("/v1/files/hash/{path}")
    public Mono<String> getFileHash(@PathVariable @ApiParam(value = "文件存储标识") String path) {
        return resourceStorage.getFileHashByPath(path);
    }

}
