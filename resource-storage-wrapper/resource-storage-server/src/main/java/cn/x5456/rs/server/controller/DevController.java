package cn.x5456.rs.server.controller;

import cn.x5456.rs.def.IResourceStorage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: dengdh@dist.com.cn
 * @date: 2021/5/14 9:11
 * @description:
 */
@Api(tags = "文件存储服务-测试功能(开发使用)")
@RestController
@RequestMapping("/rest/rs/dev")
public class DevController {
    @Autowired
    private IResourceStorage mongoResourceStorage;

    @ApiOperation("清除本地缓存")
    @DeleteMapping("/v1/local/clean")
    public void cleanLocalTemp() {
        mongoResourceStorage.cleanLocalTemp();
    }

    @ApiOperation("删库")
    @DeleteMapping("/v1/database/drop")
    public void dropDatabase() {
        mongoResourceStorage.dropMongoDatabase();
    }

    @ApiOperation("本地和数据库全清除")
    @GetMapping("/v1/all")
    public void cleanAll() {
        mongoResourceStorage.cleanLocalTemp();
        mongoResourceStorage.dropMongoDatabase();
    }
}
