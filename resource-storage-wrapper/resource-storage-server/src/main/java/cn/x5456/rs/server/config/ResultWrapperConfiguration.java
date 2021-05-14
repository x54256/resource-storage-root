package cn.x5456.rs.server.config;

import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;

@Configuration
public class ResultWrapperConfiguration {

    @Bean
    public ResultWrapper.Success success(ServerCodecConfigurer codecConfigurer, RequestedContentTypeResolver contentTypeResolver) {
        return new ResultWrapper.Success(codecConfigurer.getWriters(), contentTypeResolver);
    }

    @Bean
    public ResultWrapper.Error error(ErrorAttributes errorAttributes, ResourceProperties resourceProperties,
                                     ServerProperties serverProperties, ApplicationContext applicationContext,
                                     ServerCodecConfigurer codecConfigurer) {

        return new ResultWrapper.Error(errorAttributes, resourceProperties, serverProperties.getError(),
                applicationContext, codecConfigurer);
    }
}
