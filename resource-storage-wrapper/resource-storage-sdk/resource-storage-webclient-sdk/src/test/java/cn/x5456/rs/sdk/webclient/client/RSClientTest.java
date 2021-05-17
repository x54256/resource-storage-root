package cn.x5456.rs.sdk.webclient.client;

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
@SpringBootTest
@RunWith(SpringRunner.class)
public class RSClientTest {

    @Autowired
    private RSClient client;

    @Test
    public void uploadFile() {
        Assert.assertNotNull(client.uploadFile(new File("/Users/x5456/Desktop/1.txt")).block());
    }

    @Test
    public void downloadV2() {
        ClientResponse response = client.downloadV2("60a1d27cee925445354ae62e", HttpRange.createByteRange(0, 3)).block();
        Assert.assertEquals(4L, response.headers().contentLength().getAsLong());
    }

    @Test
    public void delete() {
        Boolean block = client.delete("60a1d27cee925445354ae62e").block();
        Assert.assertTrue(block);
    }

    @Test
    public void isExist() {
        System.out.println(client.isExist("123").block());
    }

    @Test
    public void secondPass() {
        System.out.println(client.secondPass("123", "123.tmp").block());
    }
}