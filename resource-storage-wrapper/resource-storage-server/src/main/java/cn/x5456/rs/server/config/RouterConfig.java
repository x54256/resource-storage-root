package cn.x5456.rs.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.net.URI;

/**
 * @author yujx
 * @date 2021/05/10 09:17
 */
@Configuration
public class RouterConfig {

    @Bean
    public RouterFunction<ServerResponse> initRouterFunction() {
        return RouterFunctions.route()
                .GET("/", serverRequest -> ServerResponse.permanentRedirect(URI.create("/swagger-ui/index.html")).build())
                .build();
    }

}
