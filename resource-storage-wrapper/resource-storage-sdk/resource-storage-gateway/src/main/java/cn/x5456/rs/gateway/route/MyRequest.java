package cn.x5456.rs.gateway.route;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.cloud.client.loadbalancer.reactive.Request;

/**
 * @author: dengdh@dist.com.cn
 * @date: 2021/5/7 9:48
 */
@Data
@AllArgsConstructor
public class MyRequest implements Request {
    private String host;
}
