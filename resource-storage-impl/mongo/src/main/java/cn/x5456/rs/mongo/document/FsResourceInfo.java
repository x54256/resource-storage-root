package cn.x5456.rs.mongo.document;

import cn.x5456.rs.entity.ResourceInfo;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author yujx
 * @date 2021/04/25 14:31
 */
@Data
@Document("fs.resource")
public class FsResourceInfo implements ResourceInfo {

    public static final String PATH = "id";
    public static final String METADATA_ID = "metadataId";

    @Id
    private String id;

    // 文件名
    private String fileName;

    // 文件 hash 值
    private String fileHash;

    // 元数据 id @see FileMetadata
    private String metadataId;
}
