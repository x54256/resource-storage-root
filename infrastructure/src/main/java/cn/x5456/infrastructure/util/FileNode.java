package cn.x5456.infrastructure.util;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class FileNode {

    private static final Comparator<FileNode> DEFAULT_COMPARE_STRATEGY =
            Comparator.<FileNode, Boolean>comparing(x -> !x.getDirectory()).thenComparing(x -> x.getName().toLowerCase());

    public Path path;
    public BasicFileAttributes attrs;
    public String name;
    public Boolean isDirectory;
    public Integer depth;

    public FileNode pre;

    public SortedSet<FileNode> children;

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
