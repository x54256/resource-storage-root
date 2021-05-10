package cn.x5456.rs.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author: dengdh@dist.com.cn
 * @date: 2021/5/10 15:27
 * @description: 动态路由
 */
@RestController
@RequestMapping("/rest/route")
public class RouteController {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @GetMapping("/all")
    public Flux<RouteDefinition> getAllRouteDefinition() {
        return routeDefinitionLocator.getRouteDefinitions();
    }
}
