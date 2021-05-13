package cn.x5456.rs.attachment;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ZipFileNode implements Serializable {

    public static final String PATH = "path";

    /**
     * 文件名
     */
    private String name;
    /**
     * 是否是文件夹
     */
    private Boolean isDirectory;
    /**
     * 深度
     */
    private Integer depth;
    /**
     * 子节点
     */
    private List<ZipFileNode> children;

    /**
     * 服务上存储的标识
     */
    private String path;
}
