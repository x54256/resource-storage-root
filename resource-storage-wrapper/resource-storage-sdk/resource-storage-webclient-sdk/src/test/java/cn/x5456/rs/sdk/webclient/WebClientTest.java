package cn.x5456.rs.sdk.webclient;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author yujx
 * @date 2021/05/14 11:03
 */
@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class WebClientTest {

    @Autowired
    private WebClient webClient;

    @Test
    public void test() {
        Mono<String> mono = webClient
                //方法调用，WebClient中提供了多种方法
                .method(HttpMethod.DELETE)
                //请求url
//                .uri("http://127.0.0.1:8080/rest/rs/v1/files/hash/609df6faee92da38f0063a8f")
                .uri("http://rs-server/rest/rs/v1/files/hash/609df6faee92da38f0063a8f")
//                .attribute("fileHash", "d3223218cc7b4f212ebf246314642ffa2d712410769588cf7fc8fe1a9af20708")
                //获取响应结果
                .retrieve()
                //将结果转换为指定类型
                .bodyToMono(String.class);
        //block方法返回最终调用结果，block方法是阻塞的
        System.out.println("响应结果：" + mono.block());
    }
}