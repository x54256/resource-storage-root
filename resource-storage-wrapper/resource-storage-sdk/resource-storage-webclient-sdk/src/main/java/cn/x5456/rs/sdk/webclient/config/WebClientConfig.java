package cn.x5456.rs.sdk.webclient.config;

import cn.x5456.rs.sdk.webclient.lb.ConsistentHashLoadBalancerFactory;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

/**
 * @author yujx
 * @date 2021/05/17 16:11
 */
@Configuration(proxyBeanMethods = false)
public class WebClientConfig {

    @Value("${rs.client.url:http://rs-server/rest/rs}")
    private String rsUrl;

    @Bean
    @Profile("nacos")
    public ConsistentHashLoadBalancerFactory consistentHashLoadBalancerFactory() {
        return new ConsistentHashLoadBalancerFactory();
    }

    /*
      参考：https://segmentfault.com/a/1190000021133071
     */
    @Bean
    public WebClient webClient(ObjectProvider<ConsistentHashLoadBalancerFactory> lbFunction) {
        HttpClient httpClient = HttpClient.create()
                .tcpConfiguration(tcpClient -> tcpClient.doOnConnected(connection -> {
                    //读写超时设置
                    connection.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS)).addHandlerLast(new WriteTimeoutHandler(10));
                })
                        //连接超时设置
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                        .option(ChannelOption.TCP_NODELAY, true));

        WebClient.Builder builder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> {
                    // 设置最大内存占用为 2M
                    configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024);
                    configurer.customCodecs().register(new Jackson2JsonDecoder());
                    configurer.customCodecs().register(new Jackson2JsonEncoder());
                });
        if (lbFunction.getIfUnique() != null) {
            builder.filter(lbFunction.getIfUnique());
        }

        return builder
                .baseUrl(rsUrl)
                .build();
    }
}
