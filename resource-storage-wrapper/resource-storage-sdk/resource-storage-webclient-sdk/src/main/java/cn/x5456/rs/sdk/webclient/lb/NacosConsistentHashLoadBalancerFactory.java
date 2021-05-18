package cn.x5456.rs.sdk.webclient.lb;

import cn.x5456.infrastructure.util.ConcurrentConsistentHash;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceInstance;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author yujx
 * @date 2021/05/18 10:21
 */
@Slf4j
public class NacosConsistentHashLoadBalancerFactory extends ConsistentHashLoadBalancerFactory {

    @Autowired
    private NacosServiceManager nacosServiceManager;

    @Autowired
    private NacosDiscoveryProperties properties;

    @Override
    protected ConcurrentConsistentHash<ServiceInstance> createConsistentHash(String serviceId, List<ServiceInstance> instances) {
        // 包装传入的 nacos 服务实例，构建 hash 环
        List<NacosServiceInstanceWrapper> wrapperList = NacosServiceInstanceWrapper.wrapper(instances);
        ConcurrentConsistentHash<ServiceInstance> consistentHash = new ConcurrentConsistentHash<>(VIRTUAL_NODE_COUNT, wrapperList);

        // 创建并启动服务监听器
        new NacosServiceInstanceWatch(serviceId, consistentHash).start();

        // 返回 hash 环
        return consistentHash;
    }

    class NacosServiceInstanceWatch implements EventListener {

        private final String serviceId;

        private final ConcurrentConsistentHash<ServiceInstance> circle;

        public NacosServiceInstanceWatch(String serviceId, ConcurrentConsistentHash<ServiceInstance> circle) {
            this.serviceId = serviceId;
            this.circle = circle;
        }

        public void start() {
            // 向 nacos 注册监听器
            NamingService namingService = nacosServiceManager.getNamingService(properties.getNacosProperties());
            try {
                namingService.subscribe(serviceId, properties.getGroup(), this);
            } catch (NacosException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * 注：调用这个方法的线程都是同一个所以基本不会有线程安全问题
         */
        @Override
        public void onEvent(Event event) {
            log.info("event：「{}」", event);
            if (event instanceof NamingEvent) {
                List<Instance> instances = ((NamingEvent) event).getInstances();
                List<ServiceInstance> serviceInstances = NacosServiceDiscovery.hostToServiceInstanceList(instances, serviceId);
                List<NacosServiceInstanceWrapper> wrapper = NacosServiceInstanceWrapper.wrapper(serviceInstances);
                // 更新 hash 环
                circle.reset(wrapper);
            }
        }
    }

    /**
     * 注：使用 {@link ConcurrentConsistentHash} 必须重写以下方法
     * <p>
     * {@link NacosServiceInstanceWrapper#equals(java.lang.Object)}
     * {@link NacosServiceInstanceWrapper#hashCode()}
     * {@link NacosServiceInstanceWrapper#toString()}
     */
    private static class NacosServiceInstanceWrapper extends NacosServiceInstance {

        public NacosServiceInstanceWrapper(NacosServiceInstance serviceInstance) {
            super();
            super.setServiceId(serviceInstance.getServiceId());
            super.setHost(serviceInstance.getHost());
            super.setPort(serviceInstance.getPort());
            super.setSecure(serviceInstance.isSecure());
            super.setMetadata(serviceInstance.getMetadata());
        }

        public static NacosServiceInstanceWrapper wrapper(ServiceInstance serviceInstance) {
            return new NacosServiceInstanceWrapper((NacosServiceInstance) serviceInstance);
        }

        public static List<NacosServiceInstanceWrapper> wrapper(List<ServiceInstance> serviceInstances) {
            return serviceInstances.stream().map(NacosServiceInstanceWrapper::wrapper).collect(Collectors.toList());
        }

        @Override
        public String getInstanceId() {
            String key = "nacos.instanceId";
            return super.getMetadata().get(key);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NacosServiceInstanceWrapper that = (NacosServiceInstanceWrapper) o;
            return this.getInstanceId().equals(that.getInstanceId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getInstanceId());
        }

        @Override
        public String toString() {
            return this.getInstanceId();
        }
    }

}
