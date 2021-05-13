package cn.x5456.infrastructure.util;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class FileNodeDTO implements Serializable {
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
    private List<FileNodeDTO> children;
    /**
     * 附加属性
     */
    private Map<String, Object> attachments = new HashMap<>();
}
