# resource-storage-root

## 模块代码说明

- infrastructure：基础设施模块，做一些顶层的定义，例如通用的工具包、枚举类、全局配置项等；
- resource-storage-api：接口定义模块，定义了访问资源服务的接口。
- resource-storage-core：暂时没想好
- resource-storage-impl：api 层的实现
    - mongo：reactive-mongo 的实现
- resource-storage-starter：包装 impl，实现自动装配
- resource-storage-wrapper：~~包装 api 提供更完整的服务，通过 profile 选则使用哪种实现的 starter，分为以下两种~~
    ~~1. 作为 jar 包引入 app，提供 webflux api（看看能不能用 swagger）并提供钩子。~~
        - ~~webmvc 版~~
        - ~~webflux 版 + 自动装配~~ -> 废弃原因：其实大家都会对返回值进行一次包装，那还不如让他引入 starter 自己写呢。
    2. 文件服务独立部署：
        - webflux 提供 api 作为一个服务独立部署 -> 通过 profile 选择使用哪种实现的 starter
            - webmvc 阻塞版服务
        - 提供依赖，通过 gateway 或 webclient 【负载】转发到文件服务 -> 通过 profile 选择采用集群还是单体（采用注册中心）
            - 如果是 webflux 通过 RouterFunctions 好像也行呀
            - mvc 应该也有这样的东西
