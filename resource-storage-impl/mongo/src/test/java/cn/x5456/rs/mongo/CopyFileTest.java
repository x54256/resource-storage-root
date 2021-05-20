package cn.x5456.rs.mongo;

import cn.hutool.core.io.FileUtil;
import org.junit.After;
import org.junit.Test;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * @author yujx
 * @date 2021/05/20 10:43
 */
public class CopyFileTest {

    /*
    参赛选手：

    FileInputStream -> native 方法
    BufferedInputStream -> 其实他也就是调用了 FileInputStream#write(buf, 0, count) api
    RandomAccessFile -> native 方法
    FileChannel#write() -> 看不懂，甚至比使用缓冲区（byte[]）的 FileInputStream 慢
    FileChannel#transferTo() -> 零拷贝。最快
    MappedByteBuffer -> 也算零拷贝，仅次于 transferTo
    AsynchronousChannel
     */

//    // 3.8M
//    Path filePath = Paths.get("/Users/x5456/Downloads/QQ_DOWNLOADS/选址与用地预审论证（合规性审查）功能设计0512.pptx");
//    Path targetFilePath = Paths.get("/Users/x5456/Downloads/QQ_DOWNLOADS/1.pptx");
    // 394M
    Path filePath = Paths.get("/Users/x5456/Documents/workspace/学习中~/不想学的/算法&JDK源码阅读/algs4-data.zip");
    Path targetFilePath = Paths.get("/Users/x5456/Downloads/QQ_DOWNLOADS/1.zip");
//    // 800M
//    Path filePath = Paths.get("/Users/x5456/Documents/workspace/学习中~/不想学的/算法&JDK源码阅读/归档.zip");
//    Path targetFilePath = Paths.get("/Users/x5456/Downloads/QQ_DOWNLOADS/1.zip");

    /**
     * 3.8M - 16s 870ms
     */
    @Test
    public void testFileInputStream() {
        try (
                InputStream is = Files.newInputStream(filePath, StandardOpenOption.READ);
                OutputStream os = Files.newOutputStream(targetFilePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        ) {
            int c;
            while ((c = is.read()) != -1) {
                os.write(c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 3.8M - 70 ~ 110 ms
     * 394M - 3s 71ms
     * 800M - 4s 875ms
     */
    @Test
    public void testFileInputStream_Buffer() {
        try (
                InputStream is = Files.newInputStream(filePath, StandardOpenOption.READ);
                OutputStream os = Files.newOutputStream(targetFilePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        ) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 3.8M - 170 ~ 180ms
     * 394M - 15s 989ms
     * 800M - 32s 325ms
     */
    @Test
    public void testBufferedInputStream() {
        try (
                InputStream is = new BufferedInputStream(Files.newInputStream(filePath, StandardOpenOption.READ));
                OutputStream os = new BufferedOutputStream(Files.newOutputStream(targetFilePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE))
        ) {
            int c;
            while ((c = is.read()) != -1) {
                os.write(c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 3.8M - 50 ~ 65ms
     * 394M - 1s 243ms
     * 800M - 3s 418ms
     */
    @Test
    public void testBufferedInputStream_Buffer() {
        try (
                InputStream is = new BufferedInputStream(Files.newInputStream(filePath, StandardOpenOption.READ));
                OutputStream os = new BufferedOutputStream(Files.newOutputStream(targetFilePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE))
        ) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 3.8M - 50 ~ 65ms -> 使用 buffer 版和 testBufferedInputStream（使用 buffer） 差不多，不使用和 testFileInputStream 差不多
     * 394M - 2s 663ms
     * 800M - 5s 782ms
     */
    @Test
    public void testRandomAccessFile() {
        try (
                RandomAccessFile inputFile = new RandomAccessFile(filePath.toFile(), "r");
                RandomAccessFile outputFile = new RandomAccessFile(targetFilePath.toFile(), "rw");
        ) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputFile.read(buffer)) != -1) {
                outputFile.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 3.8M - 80 ~ 90ms 还不如 byte[] 快
     * 394M - 3s
     * 800M - 5s 494ms
     */
    @Test
    public void testFileChannel_write() {
        try (
                FileChannel ic = new RandomAccessFile(filePath.toFile(), "r").getChannel();
                FileChannel oc = new RandomAccessFile(targetFilePath.toFile(), "rw").getChannel();
        ) {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (ic.read(buffer) != -1) {
                buffer.flip();
                oc.write(buffer);
                buffer.flip();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 3.8M - 30ms
     * 394M - 593ms
     * 800M - 2s 404ms
     */
    @Test
    public void testFileChannel_transferTo() {
        try (
                FileChannel ic = new RandomAccessFile(filePath.toFile(), "r").getChannel();
                FileChannel oc = new RandomAccessFile(targetFilePath.toFile(), "rw").getChannel();
        ) {
            // 操作的时候会使用操作系统的 0 拷贝进行优化，传输上限为 2G，因为 long 类型最大 2G
            ic.transferTo(0, ic.size(), oc);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 3.8M - 30~50ms
     * 394M - 1s 286ms
     * 800M - 4s 968ms
     */
    @Test
    public void testMappedByteBuffer() {
        try (
                FileChannel ic = new RandomAccessFile(filePath.toFile(), "r").getChannel();
                FileChannel oc = new RandomAccessFile(targetFilePath.toFile(), "rw").getChannel();
        ) {
            long size = ic.size();
            MappedByteBuffer mappedByteBuffer = ic.map(FileChannel.MapMode.READ_ONLY, 0, size);
            MappedByteBuffer targetMappedByteBuffer = oc.map(FileChannel.MapMode.READ_WRITE, 0, size);

            targetMappedByteBuffer.put(mappedByteBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @After
    public void after() {
        FileUtil.del(targetFilePath);
    }
}
