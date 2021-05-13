package cn.x5456.rs.gateway.route;

import cn.hutool.core.lang.ConsistentHash;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author: dengdh@dist.com.cn
 * @date: 2021/5/6 16:10
 * @description: 自定义负载策略， 参照 {@link RoundRobinLoadBalancer}
 * 这里原本想参照 {@link RoundRobinLoadBalancer} 来重写choose和getInstanceResponse的，但是请看构造函数里的两个参数从 Environment 中获取失败，
 * 所以采用了另一种方式：MyReactiveLoadBalancerClientFilter + MyRoundRobinLoadBalancer
 */
@Slf4j
public class MyRoundRobinLoadBalancer {

    private ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

    private List<ServiceInstance> instanceList;

    private final static int VIRTUAL_NODE_COUNT = 5;

    private ConsistentHash consistentHash;

    private String serviceId;

    public MyRoundRobinLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider, String serviceId) {
        this.serviceId = serviceId;
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
    }

    public Mono<Response<ServiceInstance>> choose(Request request) {
        if (serviceInstanceListSupplierProvider != null) {
            ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
                    .getIfAvailable(NoopServiceInstanceListSupplier::new);
            return supplier.get().next().map(s -> getInstanceResponse(s, request));
        }
        return null;
    }

    /**
     * 具体的策略
     *
     * @param instances 所有可用的服务实例
     */
    private Response<ServiceInstance> getInstanceResponse(
            List<ServiceInstance> instances, Request request) {
        if (instances.isEmpty()) {
            log.warn("{}没有可用的实例", serviceId);
            return new EmptyResponse();
        }

        // 判断服务是否为空
        if (CollectionUtils.isEmpty(instanceList)) {
            instanceList = instances;
        }

        // 构造hash环
        if (ObjectUtil.isNull(consistentHash)) {
            consistentHash = new ConsistentHash<>(VIRTUAL_NODE_COUNT, instanceList);
        }

        // 判断服务是否有更新
        boolean update = instances.stream().anyMatch(i -> !instanceList.contains(i));
        if (update) {
            instanceList.clear();
            instanceList = instances;
            // 直接重构hansh环
            consistentHash = new ConsistentHash<>(VIRTUAL_NODE_COUNT, instanceList);
        }

        // 根据key取第一个节点
        if (request instanceof MyRequest) {
            MyRequest myRequest = (MyRequest) request;
            ServiceInstance serviceInstance = (ServiceInstance) consistentHash.get(myRequest.getHost());
            return new DefaultResponse(serviceInstance);
        }

        // 否则就默认拿第一个
        ServiceInstance instance = instanceList.get(0);
        return new DefaultResponse(instance);
    }
}
