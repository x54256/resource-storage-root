package cn.x5456.rs.sdk.webclient.lb;

import cn.hutool.core.lang.ConsistentHash;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yujx
 * @date 2021/05/14 09:26
 */
@Slf4j
@Component
public class ConsistentHashLoadBalancerFactory implements ExchangeFilterFunction {

    private final static int VIRTUAL_NODE_COUNT = 5;

    public final static String FILE_HASH = "fileHash";

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

    // TODO: 2021/5/14 实例上线 or 下线 EventDispatcher NamingEvent  org.springframework.cloud.client.ServiceInstance
    private final Map<String, ConsistentHash<ServiceInstance>> consistentHashCircleMap = new ConcurrentHashMap<>();

    @NotNull
    @Override
    public Mono<ClientResponse> filter(ClientRequest request, @NotNull ExchangeFunction next) {
        URI originalUrl = request.url();
        String serviceId = originalUrl.getHost();

        // 如果请求的 serviceId 为空则响应 400 状态码
        if (serviceId == null) {
            String message = String.format("Request URI does not contain a valid hostname: %s", originalUrl.toString());
            log.warn(message);
            return Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST).body(message).build());
        }

        return this.choose(serviceId, request).flatMap(response -> {
            ServiceInstance instance = response.getServer();
            if (instance == null) {
                String message = "Load balancer does not contain an instance for the service " + serviceId;
                log.warn(message);
                return Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).body(message).build());
            }

            ClientRequest newRequest = buildClientRequest(request, reconstructURI(instance, originalUrl));
            return next.exchange(newRequest);
        });
    }

    private ClientRequest buildClientRequest(ClientRequest request, URI uri) {
        return ClientRequest.create(request.method(), uri)
                .headers(headers -> headers.addAll(request.headers()))
                .cookies(cookies -> cookies.addAll(request.cookies()))
                .attributes(attributes -> attributes.putAll(request.attributes()))
                .body(request.body()).build();
    }

    protected URI reconstructURI(ServiceInstance instance, URI original) {
        return LoadBalancerUriTools.reconstructURI(instance, original);
    }

    private Mono<Response<ServiceInstance>> choose(String serviceId, ClientRequest request) {

        ConsistentHash<ServiceInstance> consistentHash = consistentHashCircleMap.computeIfAbsent(serviceId, sId -> {
            List<ServiceInstance> instances = discoveryClient.getInstances(sId);
            return new ConsistentHash<>(VIRTUAL_NODE_COUNT, instances);
        });

        // 获取请求中的 fileHash 属性，如果没有则随机生成一个 uuid 值
        Object fileHash = request.attribute(FILE_HASH).orElse(null);
        if (Objects.isNull(fileHash)) {
            return this.choose(serviceId);
        }

        // 根据 serviceId 获取实例
        ServiceInstance serviceInstance = consistentHash.get(serviceId);
        if (serviceInstance == null) {
            return Mono.just(new EmptyResponse());
        }

        Response<ServiceInstance> response = new DefaultResponse(consistentHash.get(fileHash));
        return Mono.just(response);
    }

    private Mono<Response<ServiceInstance>> choose(String serviceId) {
        ReactiveLoadBalancer<ServiceInstance> loadBalancer = loadBalancerFactory.getInstance(serviceId);
        if (loadBalancer == null) {
            return Mono.just(new EmptyResponse());
        }
        return Mono.from(loadBalancer.choose());
    }
}
