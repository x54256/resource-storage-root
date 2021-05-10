package cn.x5456.rs.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author: dengdh@dist.com.cn
 * @date: 2021/5/10 15:58
 * @description: 全局日志处理，前置
 */
@Component
public class LogFilter implements GlobalFilter, Ordered {

    private static Logger logger = LoggerFactory.getLogger(LogFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.info("主机[{}]正在访问，请求路径为[{}]",exchange.getRequest().getRemoteAddress().toString(),exchange.getRequest().getURI());
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
