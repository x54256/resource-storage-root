package cn.x5456.rs.server.config;

import cn.x5456.rs.common.ResponseData;
import cn.x5456.rs.common.UnWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public final class ResultWrapper {
    static Logger LOGGER = LoggerFactory.getLogger(ResultWrapper.class);

    public ResultWrapper() {
    }

    @Slf4j
    public static class Success extends ResponseBodyResultHandler {

        private final static MethodParameter METHOD_PARAMETER_MONO_COMMON_RESULT;

        static {
            try {
                // 获得 METHOD_PARAMETER_MONO_COMMON_RESULT 。其中 -1 表示 `#methodForParams()` 方法的返回值
                METHOD_PARAMETER_MONO_COMMON_RESULT = new MethodParameter(
                        Success.class.getDeclaredMethod("methodForParams"), -1);
            } catch (NoSuchMethodException e) {
                log.error("[static][获取 METHOD_PARAMETER_MONO_COMMON_RESULT 时，找不都方法");
                throw new RuntimeException(e);
            }
        }

        public Success(List<HttpMessageWriter<?>> writers, RequestedContentTypeResolver resolver) {
            super(writers, resolver);
        }

        public Success(List<HttpMessageWriter<?>> writers, RequestedContentTypeResolver resolver, ReactiveAdapterRegistry registry) {
            super(writers, resolver, registry);
        }

        public boolean supports(HandlerResult result) {
            MethodParameter returnType = result.getReturnTypeSource();
            return !returnType.hasMethodAnnotation(UnWrapper.class);
        }

        @NotNull
        @Override
        public Mono<Void> handleResult(@NotNull ServerWebExchange exchange, HandlerResult result) {
            Object realResult = result.getReturnValue();
            Publisher<?> wrapperResult;
            if (realResult instanceof Mono) {
                wrapperResult = ((Mono<?>) realResult).map(ResponseData::success);
            } else if (realResult instanceof Flux) {
                wrapperResult = ((Flux<?>) realResult).collectList().map(ResponseData::success);
            } else {
                wrapperResult = Mono.just(ResponseData.success(realResult));
            }

            return this.writeBody(wrapperResult, METHOD_PARAMETER_MONO_COMMON_RESULT, exchange);
        }

        private static Mono<ResponseData> methodForParams() {
            return null;
        }

    }

    public static class Error extends DefaultErrorWebExceptionHandler {
        public Error(ErrorAttributes errorAttributes, ResourceProperties resourceProperties,
                     ErrorProperties errorProperties, ApplicationContext applicationContext) {

            super(errorAttributes, resourceProperties, errorProperties, applicationContext);
        }

        public Error(ErrorAttributes errorAttributes, ResourceProperties resourceProperties,
                     ErrorProperties errorProperties, ApplicationContext applicationContext,
                     ServerCodecConfigurer codecConfigurer) {

            this(errorAttributes, resourceProperties, errorProperties, applicationContext);
            this.setMessageWriters(codecConfigurer.getWriters());
        }

        protected Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
            Throwable error = this.getError(request);
            ResultWrapper.LOGGER.error(error.getMessage(), error);
            ResponseData response = ResponseData.fail(error);
            return ServerResponse.status(response.getStatus()).contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(response));
        }

        protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
            return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
        }
    }


}
