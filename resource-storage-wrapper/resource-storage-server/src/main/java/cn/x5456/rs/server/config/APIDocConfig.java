package cn.x5456.rs.server.config;

import com.fasterxml.classmate.TypeResolver;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.WebSession;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Data
@Configuration
@EnableOpenApi
public class APIDocConfig {
    static final Logger LOGGER = LoggerFactory.getLogger(APIDocConfig.class);
    private boolean enable = true;
    private String name = "XDATA";
    private String description = "上海数慧数据中心接口文档";
    private String website = "http://www.dist.com.cn";
    private String author = "xdata@dist.com.cn";
    private String version = "1.0.0-SNAPSHOT";

    @Autowired
    private TypeResolver typeResolver;

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.OAS_30)
                .ignoredParameterTypes(ServerHttpRequest.class, ServerHttpResponse.class, WebSession.class)
                .useDefaultResponseMessages(false)
                .pathMapping("/")
                .enable(this.enable)
                .apiInfo(this.apiInfo())
//                //自定义规则，如果遇到 DeferredResult，则把泛型类转成json
//                .alternateTypeRules(
//                        AlternateTypeRules.newRule(
//                                typeResolver.resolve(DeferredResult.class, typeResolver.resolve(ResponseEntity.class, WildcardType.class)),
//                                typeResolver.resolve(WildcardType.class)))
                .select()
                .apis(RequestHandlerSelectors.basePackage("cn.x5456.rs.server.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        Contact contact = new Contact(this.name, this.website, this.author);
        return (new ApiInfoBuilder()).title(this.name).description(this.description).license("禁止无关人员调用及恶意调用！").contact(contact).version(this.version).build();
    }
}
