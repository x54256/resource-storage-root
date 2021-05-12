package cn.x5456.infrastructure.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.compress.CompressUtil;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * @author yujx
 * @date 2021/05/11 17:25
 */
public final class CompressUtils {

    /**
     * 只支持
     * <ul>
     *     <li>{@link ArchiveStreamFactory#AR}</li>
     *     <li>{@link ArchiveStreamFactory#CPIO}</li>
     *     <li>{@link ArchiveStreamFactory#JAR}</li>
     *     <li>{@link ArchiveStreamFactory#TAR}</li>
     *     <li>{@link ArchiveStreamFactory#ZIP}</li>
     *     <li>{@link ArchiveStreamFactory#SEVEN_Z}</li>
     * </ul>
     */
    public static void extract(String compressFilePath, String targetDir) {
        CompressUtil.createExtractor(StandardCharsets.UTF_8, new File(compressFilePath))
                .extract(new File(targetDir));
    }

    public static String extract(String compressFilePath) {
        int i = FileUtil.getSuffix(compressFilePath).length() + 1;
        String targetDir = compressFilePath.substring(0, compressFilePath.length() - i);
        extract(compressFilePath, targetDir);
        return targetDir;
    }

    private CompressUtils() {
    }
}
