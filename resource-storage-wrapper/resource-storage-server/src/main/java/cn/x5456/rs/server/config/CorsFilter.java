package cn.x5456.rs.server.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Data
@Component
@Order(-2147483648)
@ConfigurationProperties("x5456.rs.cors")
public class CorsFilter implements WebFilter {

    private List<String> origins = Collections.singletonList("*");
    private List<String> methods = Arrays.asList(HttpMethod.OPTIONS.name(), HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(), HttpMethod.DELETE.name());
    private List<String> headers = Collections.singletonList("Content-Type");

    @NotNull
    public Mono<Void> filter(ServerWebExchange exchange, @NotNull WebFilterChain chain) {
        exchange.getAttributes().put("startTime", new Date());
        if (!CorsUtils.isCorsRequest(exchange.getRequest())) {
            return chain.filter(exchange);
        } else {
            HttpHeaders requestHeaders = exchange.getRequest().getHeaders();
            HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
            responseHeaders.add("Access-Control-Allow-Origin", this.checkAndGetOrigin(requestHeaders.getOrigin()));
            responseHeaders.add("Access-Control-Allow-Headers", StrUtil.join(",", this.headers));
            responseHeaders.add("Access-Control-Allow-Methods", StrUtil.join(",", this.methods));
            responseHeaders.add("Access-Control-Allow-Credentials", "true");
            if (CorsUtils.isPreFlightRequest(exchange.getRequest())) {
                exchange.getResponse().setStatusCode(HttpStatus.OK);
                return Mono.empty();
            } else {
                return chain.filter(exchange);
            }
        }
    }

    public String checkAndGetOrigin(String requestOrigin) {
        if (StrUtil.isBlank(requestOrigin)) {
            return "*";
        } else if (this.origins.contains("*")) {
            return requestOrigin;
        } else {
            for (String origin : origins) {
                if (requestOrigin.equalsIgnoreCase(origin)) {
                    return requestOrigin;
                }
            }
            throw new RuntimeException("Not Allowed Origin!");
        }
    }
}