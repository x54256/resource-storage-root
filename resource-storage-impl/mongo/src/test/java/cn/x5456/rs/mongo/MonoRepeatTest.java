package cn.x5456.rs.mongo;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yujx
 * @date 2021/05/10 13:41
 */
@Slf4j
public class MonoRepeatTest {

    /*
    正常

    16:38:21.861 [main] INFO cn.x5456.rs.mongo.MonoRepeatTest - x：「5」
    16:38:23.274 [parallel-1] INFO cn.x5456.rs.mongo.MonoRepeatTest - x：「6」  1s多
    16:38:24.785 [parallel-2] INFO cn.x5456.rs.mongo.MonoRepeatTest - x：「7」  1.5s
    16:38:29.874 [parallel-3] INFO cn.x5456.rs.mongo.MonoRepeatTest - x：「8」  5s 多
     */
    @Test(expected = RuntimeException.class)
    public void test() {
        AtomicInteger atomicInteger = new AtomicInteger(5);

        Mono.just(atomicInteger)
                .flatMap(x -> {
                    log.info("x：「{}」", x);
                    if (atomicInteger.getAndIncrement() == 8) {
                        return Mono.just(x);
                    }
                    return Mono.error(new RuntimeException("test retry"));
                })
                .retryBackoff(5, Duration.ofSeconds(1), Duration.ofSeconds(2))
                .onErrorResume((ex) -> Mono.error(ex.getCause()))
                .block();
    }

    @Test(expected = RuntimeException.class)
    public void testV2() {
        AtomicInteger atomicInteger = new AtomicInteger(5);

        RetryBackoffSpec retryBackoffSpec = Retry.backoff(5, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(2));
        Mono.just(atomicInteger)
                .flatMap(x -> {
                    log.info("x：「{}」", x);
                    if (atomicInteger.getAndIncrement() == 8) {
                        return Mono.just(x);
                    }
//                    return Mono.empty();
                    return Mono.error(new RuntimeException("test retry"));
                })
                .retryWhen(retryBackoffSpec)
                .onErrorResume((ex) -> Mono.error(ex.getCause()))
                .switchIfEmpty(Mono.fromRunnable(() -> System.out.println("空的")))
                .block();
    }

    /*
    1. retry()和retryWhen()
    当onError()回调时，触发重复订阅流程

    2. repeat()和repeatWhen()
    当onCompleted()回调时，触发重复订阅流程
     */
}
