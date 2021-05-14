package cn.x5456.rs.sdk.webclient.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Autowired
    private ConsistentHashLoadBalancerFactory lbFunction;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .filter(lbFunction)
                .build();
    }
}