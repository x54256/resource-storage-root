package cn.x5456.infrastructure.util;

import cn.hutool.core.io.FileUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * @author yujx
 * @date 2021/05/20 13:19
 */
public class FileChannelUtilTest {

    // 20b
    String chunk1 = FileUtil.getAbsolutePath("chunk1.tmp");

    // 7b
    String chunk2 = FileUtil.getAbsolutePath("chunk2.tmp");

    @Test
    public void test() {
        String targetFile = FileUtil.getAbsolutePath("target1.tmp");
        FileChannelUtil.transferFrom(chunk1, targetFile, 0, 2);
        Assert.assertEquals(2, new File(targetFile).length());
    }

    @Test
    public void test2() {
        String targetFile = FileUtil.getAbsolutePath("target2.tmp");
        FileChannelUtil.transferFrom(chunk1, targetFile, 0, 2);
        FileChannelUtil.transferFrom(chunk2, targetFile, 2, 4);
        Assert.assertEquals(6, new File(targetFile).length());
    }
}