package cn.x5456.rs.sdk.webclient.client;

import cn.hutool.core.io.FileUtil;
import cn.x5456.rs.entity.ResourceInfo;
import cn.x5456.rs.sdk.webclient.BootstrapConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpRange;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.io.File;

/**
 * @author yujx
 * @date 2021/05/17 09:56
 */
@SpringBootTest(classes = BootstrapConfig.class)
@RunWith(SpringRunner.class)
public class RSClientTest {

    @Autowired
    private RSClient client;

    @Test
    public void downloadV2() {
        ResourceInfo resourceInfo = client.uploadFile(new File(FileUtil.getAbsolutePath("1.txt"))).block();
        ClientResponse response = client.downloadV2(resourceInfo.getId(), HttpRange.createByteRange(0, 3)).block();
        Assert.assertEquals(4L, response.headers().contentLength().getAsLong());
    }
}