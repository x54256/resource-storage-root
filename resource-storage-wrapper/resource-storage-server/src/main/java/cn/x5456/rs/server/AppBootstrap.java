package cn.x5456.rs.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @author yujx
 * @date 2021/01/15 09:26
 */
@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
public class AppBootstrap {

    public static void main(String[] args) {
        SpringApplication.run(AppBootstrap.class, args);
    }
}
