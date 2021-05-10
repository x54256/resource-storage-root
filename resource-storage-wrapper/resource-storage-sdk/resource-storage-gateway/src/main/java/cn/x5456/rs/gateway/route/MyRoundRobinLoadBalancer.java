package cn.x5456.rs.gateway.route;

import cn.x5456.infrastructure.util.HashUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.reactive.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.reactive.Request;
import org.springframework.cloud.client.loadbalancer.reactive.Response;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author: dengdh@dist.com.cn
 * @date: 2021/5/6 16:10
 * @description: 自定义负载策略， 参照 {@link RoundRobinLoadBalancer}
 * 这里原本想参照 {@link RoundRobinLoadBalancer} 来重写choose和getInstanceResponse的，但是请看构造函数里的两个参数从 Environment 中获取失败，
 * 所以采用了另一种方式：MyReactiveLoadBalancerClientFilter + MyRoundRobinLoadBalancer
 */
public class MyRoundRobinLoadBalancer extends RoundRobinLoadBalancer {

    private ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

    private List<ServiceInstance> instanceList;

    private ConcurrentMap<String, List<ServiceInstance>> serviceMap;

    private final static int virtualNode = 5;
    /**
     * 各个虚拟节点集合
     */
    private static SortedMap<Integer, String> virtualNodeMap = new TreeMap<>();

    public MyRoundRobinLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider, String serviceId) {
        super(serviceInstanceListSupplierProvider, serviceId);
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
    }

    @Override
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
            return new EmptyResponse();
        }

        // 判断服务是否为空
        if (CollectionUtils.isEmpty(instanceList)) {
            instanceList = instances;
            buildVirtualNodeMap();
        }

        // 判断服务是否有更新
        boolean update = instances.stream().anyMatch(i -> !instanceList.contains(i));
        if (update) {
            instanceList.clear();
            instanceList = instances;
            buildVirtualNodeMap();
        }

        if (request instanceof MyRequest) {
            MyRequest myRequest = (MyRequest) request;
            String server = getServer(myRequest.getHost());
            ServiceInstance serviceInstance = serviceMap.get(server).get(0);
            return new DefaultResponse(serviceInstance);
        }

        // 否则就默认拿第一个
        ServiceInstance instance = instanceList.get(0);
        return new DefaultResponse(instance);
    }

    /**
     * 获取服务的 key
     *
     * @param node 客户端 ip
     * @return 服务实例的 key
     */
    private String getServer(String node) {
        Integer nodeCode = HashUtil.FNV1_32_HASH(node);
        // 获取大于当前hash值的节点集合
        SortedMap<Integer, String> tailMap = virtualNodeMap.tailMap(nodeCode);
        Integer key;
        if (tailMap.isEmpty()) {
            key = virtualNodeMap.firstKey();
        } else {
            key = tailMap.firstKey();
        }
        // 拿到值
        String virtualNodeValue = virtualNodeMap.get(key);
        return virtualNodeValue.split("&&")[0];
    }

    /**
     * 构建虚拟节点
     */
    private void buildVirtualNodeMap() {
        virtualNodeMap.clear();
        // 根据服务实例id分组   instanceId：192.168.1.99:8501
        serviceMap = instanceList.stream().collect(Collectors.groupingByConcurrent(ServiceInstance::getInstanceId));
        Set<String> keySet = serviceMap.keySet();

        // 虚拟节点
        keySet.forEach(k -> {
            IntStream.range(0, 4).forEach(i -> {
                String virtualNodeName = k + "&&Node" + i;
                Integer hashCode = HashUtil.FNV1_32_HASH(virtualNodeName);
                virtualNodeMap.put(hashCode, virtualNodeName);
            });
        });
    }
}
