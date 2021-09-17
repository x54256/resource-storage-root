package cn.x5456.infrastructure.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * @author yujx
 * @date 2021/09/17 13:11
 */
public class FileMimeTypeUtil {

    /**
     * 支持的视频文件格式
     */
    public static final List<String> VIDEO_EXTENSIONS = Arrays.asList(
            "mp4", "flv", "wmv", "avi", "webm", "3gp", "ogv"
    );

    /**
     * 支持的音频文件格式
     */
    public static final List<String> AUDIO_EXTENSIONS = Arrays.asList(
            "mp3", "wav", "ogg"
    );

    /**
     * 支持的图片文件格式
     */
    public static final List<String> IMAGE_EXTENSIONS = Arrays.asList(
            "png", "jpg", "gif", "jpeg", "bmp", "tiff", "svg", "webp"
    );

    @Nullable
    public static String getMimeType(String nameOrPath) {
        String mimeType = FileUtil.getMimeType(nameOrPath);
        if (StrUtil.isNotBlank(mimeType)) {
            return mimeType;
        }

        // 获取文件后缀
        String extension = FileUtil.getSuffix(nameOrPath);
        if (IMAGE_EXTENSIONS.contains(extension)) {
            return "image/" + extension;
        }
        if (VIDEO_EXTENSIONS.contains(extension)) {
            return "video/" + extension;
        }
        if (AUDIO_EXTENSIONS.contains(extension)) {
            return "audio/" + extension;
        }

        return null;
    }
}
