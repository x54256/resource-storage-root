package cn.x5456.infrastructure.util;

import org.springframework.data.annotation.Transient;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class FileNode implements Serializable {

    private static final Comparator<FileNode> DEFAULT_COMPARE_STRATEGY =
            Comparator.<FileNode, Boolean>comparing(x -> !x.getDirectory()).thenComparing(x -> x.getName().toLowerCase());

    private Path path;
    private BasicFileAttributes attrs;
    private String name;
    private Boolean isDirectory;
    private Integer depth;

    @Transient
    private transient FileNode pre;

    private SortedSet<FileNode> children;

    private Map<String, Object> attachments = new HashMap<>();

    public FileNode() {
        this(DEFAULT_COMPARE_STRATEGY);
    }

    public FileNode(Comparator<FileNode> comparator) {
        children = new TreeSet<>(comparator);
    }

    public void addChildNode(FileNode fileNode) {
        children.add(fileNode);
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public BasicFileAttributes getAttrs() {
        return attrs;
    }

    public void setAttrs(BasicFileAttributes attrs) {
        this.attrs = attrs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getDirectory() {
        return isDirectory;
    }

    public void setDirectory(Boolean directory) {
        isDirectory = directory;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public FileNode getPre() {
        return pre;
    }

    public void setPre(FileNode pre) {
        this.pre = pre;
    }

    public SortedSet<FileNode> getChildren() {
        return children;
    }

    public Map<String, Object> getAttachments() {
        return attachments;
    }

    public void putAttachment(String key, Object value) {
        this.attachments.put(key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileNode fileNode = (FileNode) o;
        return Objects.equals(path, fileNode.path) &&
                Objects.equals(attrs, fileNode.attrs) &&
                Objects.equals(name, fileNode.name) &&
                Objects.equals(isDirectory, fileNode.isDirectory) &&
                Objects.equals(depth, fileNode.depth) &&
                Objects.equals(children, fileNode.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, attrs, name, isDirectory, depth, children);
    }
}
