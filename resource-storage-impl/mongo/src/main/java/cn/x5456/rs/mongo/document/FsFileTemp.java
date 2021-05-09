package cn.x5456.rs.mongo.document;

import cn.x5456.rs.def.UploadProgress;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 只用于分片上传
 *
 * @author yujx
 * @date 2021/04/25 17:34
 */
@Data
@Document("fs.temp")
@CompoundIndexes(@CompoundIndex(name = "hash_chunk_unique_index", def = "{'fileHash' : 1, 'chunk': 1}", unique = true))
public class FsFileTemp {

    public static final String ID = "id";
    public static final String FILE_HASH = "fileHash";
    public static final String CHUNK = "chunk";
    public static final String UPLOAD_PROGRESS = "uploadProgress";
    public static final String CREAT_TIME = "creatTime";

    @Id
    private String id;

    // 文件 hash 值，建议使用 sha256
    private String fileHash;

    /**
     * 第几片
     * <p>
     * 此字段需与 {@link FsFileMetadata.FsFilesInfo#chunk} 相同
     */
    private Integer chunk;

    /**
     * 对应的 fs.files 表的 id
     * <p>
     * 此字段需与 {@link FsFileMetadata.FsFilesInfo#filesId} 相同
     */
    private String filesId;

    /**
     * 当前片的大小
     * <p>
     * 此字段需与 {@link FsFileMetadata.FsFilesInfo#chunkSize} 相同
     */
    private Long chunkSize;

    // 上传进度
    private UploadProgress uploadProgress;

    /**
     * 创建时间
     */
    private LocalDateTime creatTime;
}
