package cn.x5456.infrastructure.util;

import cn.hutool.core.io.IORuntimeException;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

/**
 * @author yujx
 * @date 2021/05/20 13:03
 */
public final class FileChannelUtil {

    /**
     * 通过给定的 filePath 创建一个只读的 FileChannel
     *
     * @param filePath 文件路径
     * @return 只读的 FileChannel
     */
    public static FileChannel newReadableFileChannel(String filePath) {
        try {
            return new RandomAccessFile(filePath, "r").getChannel();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    /**
     * 通过给定的 filePath 创建一个可读写的 FileChannel，如果文件不存在则会创建
     *
     * @param filePath 文件路径
     * @return 可读写的 FileChannel
     */
    public static FileChannel newWritableFileChannel(String filePath) {
        try {
            return new RandomAccessFile(filePath, "rw").getChannel();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    /**
     * 从 form 的 position 位置开始 copy count 个字节到 to
     *
     * @param form     源文件
     * @param to       目标文件
     * @param position 文件在文件中开始传输的位置；必须为非负数
     * @param count    传输的最大字节数；必须为非负数
     */
    public static void transferFrom(String form, String to, long position, long count) {
        try (
                FileChannel formChannel = newReadableFileChannel(form);
                FileChannel toChannel = newWritableFileChannel(to)
        ) {
            toChannel.transferFrom(formChannel, position, count);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将源文件 copy 到目标文件
     *
     * @param form 源文件
     * @param to   目标文件
     */
    public static void transferFrom(String form, String to) {
        try (
                FileChannel formChannel = newReadableFileChannel(form);
                FileChannel toChannel = newWritableFileChannel(to)
        ) {
            formChannel.transferTo(0, formChannel.size(), toChannel);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void transferFrom(String form, FileChannel toChannel) {
        try (
                FileChannel formChannel = newReadableFileChannel(form)
        ) {
            formChannel.transferTo(0, formChannel.size(), toChannel);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private FileChannelUtil() {
    }
}
