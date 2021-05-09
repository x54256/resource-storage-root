package cn.x5456.rs.mongo;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.TimeUnit;

/**
 * @author yujx
 * @date 2021/05/09 18:01
 */
@Slf4j
public class FileLockTest {

    /*
    https://www.cnblogs.com/DreamDrive/p/7425281.html

    0. linux会抛出异常：【java.io.IOException: Permission denied 】 -> 未测试
    1. 如果重叠会报错 OverlappingFileLockException
    2. 对于独占锁来说 lock 和 tryLock 都会立即报错 OverlappingFileLockException
    3. 线程 a 获取锁了，线程 b 不获取锁也是可以写入数据的（MAC）
        文件锁的效果是与操作系统相关的。一些系统中文件锁是强制性的，就当Java的某进程获得文件锁后，操作系统将保证其它进程无法对文件做操作了。
        而另一些操作系统的文件锁是询问式的(advisory)，意思是说要想拥有进程互斥的效果，其它的进程也必须也按照API所规定的那样来申请或者检测文件锁，
        不然将起不到进程互斥的功能。所以文档里建议将所有系统都当做是询问式系统来处理，这样程序更加安全也更容易移植。
     */
    @Test
    public void test() throws Exception {
        String s = "/Users/x5456/Desktop/1.txt";
        Thread t1 = new Thread(() -> {
            try {
                RandomAccessFile randomAccessFile = new RandomAccessFile(s, "rw");
                FileChannel channel = randomAccessFile.getChannel();
                FileLock lock = channel.lock(0, 5, false);
                log.info("t1 线程获取到排他锁了！");
                lock.release();
                log.info("t1 线程释放了排他锁了！");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                RandomAccessFile randomAccessFile = new RandomAccessFile(s, "rw");
                FileChannel channel = randomAccessFile.getChannel();
                FileLock lock = channel.lock(5, 29, false);
                log.info("t2 线程获取到排他锁了！");
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                lock.release();
                log.info("t2 线程释放了排他锁了！");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        t1.start();
        t2.start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        RandomAccessFile randomAccessFile = new RandomAccessFile(s, "rw");
        FileChannel channel = randomAccessFile.getChannel();
//        channel.write(ByteBuffer.wrap("123456789abcdefg".getBytes(StandardCharsets.UTF_8)));
//        log.info("数据写入成功");
        System.out.println(channel.tryLock(5, 29, true));

        t1.join();
        t2.join();
    }

    /*
    t1  t2


    方案一：
    1. t1 线程先检查 official 文件是否存在
        1.1 如果不存在，则尝试获取 tmp 文件的锁
            1.1.1 获取成功，代表没有线程在下载
                检查 official 文件是否存在，如果存在则返回 official
                如果不存在，则"释放锁"，重新为每一段加锁，进行下载 -> 重新加锁的时候报错怎么办？？？
        1.2. t2 线程获取 tmp 锁，发现获取不到，则等待；之后获取到了代表要不 t1 下载成功要不失败，则重新检测 official 是否存在，回到了 1.1.1

    > 重新加锁的时候报错怎么办？？？所以上面的方案不行，不能加整个文件的锁。

    方案二：分段加锁，不论哪个线程都可以写 ->  两个请求写入同一个文件又不知道是不是真的写入成功了。

    方案三：加一个 lock 文件 + 方案一 - 不对 tmp 文件加分段锁了  -> 保证只有一个请求在进行下载

    0. while(true) 尝试加锁
    1. 检查 official 文件是否存在
    2. 如果不存在尝试对 lock 文件加锁
    3. 如果加锁成功，则执行接下来的逻辑，
        逻辑执行成功则文件名"转正"并释放锁
        执行失败，则删除 tmp 文件并释放锁
    4. 如果加锁失败，则 while(true) 等待
    5. 等待之后获取成功，检查 official 文件是否存在，回到 1

    注：如果下载过程中挂了，再次重启时会清理文件夹，不用考虑
     */
}
