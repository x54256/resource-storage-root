package cn.x5456.rs.def.block;

import cn.hutool.core.lang.Pair;
import cn.x5456.rs.entity.ResourceInfo;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author yujx
 * @date 2021/05/08 10:09
 */
public interface IBlockResourceStorage {

    ResourceInfo uploadFile(String localFilePath, String path);

    ResourceInfo uploadFile(String localFilePath, String fileName, String path);

    ResourceInfo uploadFile(InputStream inputStream, String fileName, String path);

    Boolean downloadFile(String localFilePath, String path);

    Pair<String, OutputStream> downloadFile(String path);

    Boolean deleteFile(String path);

    String getFileName(String path);

    BlockBigFileUploader getBigFileUploader();
}
