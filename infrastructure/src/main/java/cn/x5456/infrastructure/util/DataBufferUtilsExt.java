package cn.x5456.infrastructure.util;

import org.reactivestreams.Publisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author yujx
 * @date 2021/05/08 19:51
 */
public final class DataBufferUtilsExt {

    private DataBufferUtilsExt() {
    }

    public static Flux<DataBuffer> read(Resource resource, long rangeStart, long rangeEnd, DataBufferFactory bufferFactory, int bufferSize) {
        Flux<DataBuffer> bufferFlux = DataBufferUtils.read(resource, bufferFactory, bufferSize);
        return DataBufferUtilsExt.skipUntilByteCountExt(bufferFlux, rangeStart, rangeEnd);
    }

    public static Flux<DataBuffer> skipUntilByteCountExt(Publisher<? extends DataBuffer> publisher, long rangeStart, long rangeEnd) {
        Assert.notNull(publisher, "Publisher must not be null");
        Assert.isTrue(rangeStart >= 0, "'maxByteCount' must be a positive number");

        return Flux.defer(() -> {
            AtomicLong countDown = new AtomicLong(rangeStart);
            AtomicLong end = new AtomicLong(rangeEnd);
            AtomicBoolean endFlag = new AtomicBoolean(false);
            return Flux.from(publisher)
                    .skipUntil(buffer -> {
                        long remainder = countDown.addAndGet(-buffer.readableByteCount());
                        return remainder < 0;
                    })
                    .map(buffer -> {
                        long remainder = countDown.get();
                        if (remainder < 0) {
                            countDown.set(0);
                            int start = buffer.readableByteCount() + (int) remainder;
                            int length = (int) -remainder;
                            return buffer.slice(start, length);
                        } else {
                            return buffer;
                        }
                    })
                    .flatMap(buffer -> {
                        if (endFlag.get()) {
                            return Mono.empty();
                        }

                        long l = end.addAndGet(-buffer.readableByteCount());
                        if (l <= 0) {
                            endFlag.set(true);
                            // 一个 buffer 的容量为 1024，算出的 l 为 -100 那么应该读 0 ~ 1014 这部分
                            int i = buffer.readableByteCount() + (int) l - 1;
                            return Mono.just(buffer.slice(0, i));
                        }
                        return Mono.just(buffer);
                    });
        }).doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
    }

/*
    public static void main(String[] args) throws FileNotFoundException {
        Flux<DataBuffer> read = DataBufferUtils.read(new FileSystemResource("/Users/x5456/Desktop/1.txt"),
                new DefaultDataBufferFactory(), 4);

        Flux<DataBuffer> dataBufferFlux = DataBufferUtilsExt.skipUntilByteCount(read, 1, 70).log();
        FileChannel channel = new RandomAccessFile("/Users/x5456/Desktop/2.txt", "rw").getChannel();
        DataBufferUtils.write(dataBufferFlux, channel).blockLast();
    }
*/
}
