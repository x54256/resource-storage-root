package cn.x5456.rs.mongo.document;

import cn.x5456.rs.def.UploadProgress;
import cn.x5456.rs.entity.FileMetadata;
import cn.x5456.rs.entity.FilesInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yujx
 * @date 2021/04/25 16:56
 */
@Data
@Document("fs.metadata")
public class FsFileMetadata implements FileMetadata {

    public static final String FILE_HASH = "id";
    public static final String UPLOAD_PROGRESS = "uploadProgress";
    public static final String CREAT_TIME = "creatTime";
    public static final String MULTIPART_UPLOAD = "multipartUpload";

    @Id
    private String id;

    /**
     * 文件 hash 值，建议使用 sha256
     *
     * @deprecated {@link FsFileMetadata#id}
     */
    @Deprecated
    @Indexed(unique = true)
    private String fileHash;

    // 推测文件类型，如果没有推测出则设置为 null（不建议使用）
//    @Deprecated
//    private String fileType;

    // 文件的引用计数，仅供参考 todo cas 更新 https://blog.csdn.net/chinatopno1/article/details/108916905
    private Integer referenceCount;

    // 当前文件对应 fs.files 表中的多少行记录
    private Integer totalNumberOfChunks;

    // 当前文件与 fs.files 表的关联信息；使用临时表解决线程安全问题  {@link cn.x5456.rs.pre.document.FsFileTemp}
    private List<FsFilesInfo> filesInfoList;

    // 其他元数据信息
    private Map<String, Object> attachments = new HashMap<>();

    // 上传进度
    private UploadProgress uploadProgress;

    /**
     * 创建时间
     */
    private LocalDateTime creatTime;

    /**
     * 是否是分片上传
     */
    private Boolean multipartUpload;

    @Data
    @AllArgsConstructor
    public static class FsFilesInfo implements FilesInfo {

        // 第几片
        private Integer chunk;

        // 对应的 fs.files 表的 id
        private String filesId;

        // 当前片的大小
        private Long chunkSize;
    }
}
