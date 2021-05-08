package cn.x5456.infrastructure.util;

import org.springframework.http.HttpRange;

import static cn.hutool.core.convert.Convert.toLong;

public class FileDownloadUtil {

    public static String filePath(String name) {
        return "static/files/" + name + ".txt";
    }

    public static HttpRange getHttpRange(String range) {
        String[] ranges = range.substring(range.indexOf("=") + 1).split("-");
        return HttpRange.createByteRange(toLong(ranges[0]), toLong(ranges[1]));
    }

    public static String createContentRange(HttpRange httpRange, long length) {
        return "bytes" + httpRange.getRangeEnd(length) + "-" + httpRange.getRangeEnd(length) + "/" + length;
    }

}