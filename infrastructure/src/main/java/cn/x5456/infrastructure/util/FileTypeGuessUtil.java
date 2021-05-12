package cn.x5456.infrastructure.util;

import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.FileUtil;

/**
 * @author yujx
 * @date 2021/05/11 16:28
 */
public final class FileTypeGuessUtil {

    private FileTypeGuessUtil() {
    }

    static {
        FileTypeUtil.putFileType("377ABCAF271C", "7z");
    }

    public static String getTypeByPath(String localTempPath, String fileName) {
        String typeName = FileTypeUtil.getTypeByPath(localTempPath);
        if (fileName == null) {
            return typeName;
        }

        if (null == typeName) {
            // 未成功识别类型，扩展名辅助识别
            typeName = FileUtil.extName(fileName);
        } else if ("xls".equals(typeName)) {
            // xls、doc、msi的头一样，使用扩展名辅助判断
            final String extName = FileUtil.extName(fileName);
            if ("doc".equalsIgnoreCase(extName)) {
                typeName = "doc";
            } else if ("msi".equalsIgnoreCase(extName)) {
                typeName = "msi";
            }
        } else if ("zip".equals(typeName)) {
            // zip可能为docx、xlsx、pptx、jar、war等格式，扩展名辅助判断
            final String extName = FileUtil.extName(fileName);
            if ("docx".equalsIgnoreCase(extName)) {
                typeName = "docx";
            } else if ("xlsx".equalsIgnoreCase(extName)) {
                typeName = "xlsx";
            } else if ("pptx".equalsIgnoreCase(extName)) {
                typeName = "pptx";
            } else if ("jar".equalsIgnoreCase(extName)) {
                typeName = "jar";
            } else if ("war".equalsIgnoreCase(extName)) {
                typeName = "war";
            }
        }
        return typeName;
    }
}
