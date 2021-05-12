package cn.x5456.infrastructure.util;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

//@Data
@Builder
//@NoArgsConstructor
public class FileNode {
    public Path path;
    public BasicFileAttributes attrs;
    public String name;
    public Boolean isDirectory;
    public Integer depth;

    public FileNode pre;

    public SortedSet<FileNode> next = new TreeSet<>(Comparator.comparing(FileNode::getDirectory).thenComparing(FileNode::getName));

    public void addChildNode(FileNode fileNode) {
        next.add(fileNode);
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

    public SortedSet<FileNode> getNext() {
        return next;
    }
}
