package cn.x5456.infrastructure.util;

import lombok.Data;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

@Data
public class FileNodeDTO implements Serializable {

    private BasicFileAttributes attrs;
    private String name;
    private Boolean isDirectory;
    private Integer depth;

    private SortedSet<FileNodeDTO> children;

    private Map<String, Object> attachments = new HashMap<>();
}
