package cn.x5456.infrastructure.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import lombok.Data;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author: dengdh@dist.com.cn
 * @date: 2021/5/12 14:27
 * @description:
 */
public final class FileNodeUtil {

    private FileNodeUtil() {
    }

    /**
     * 递归遍历目录并处理目录下的文件，可以处理目录或文件：
     * <ul>
     * <li>非目录则直接调用{@link Consumer}处理</li>
     * <li>目录则递归调用此方法处理</li>
     * </ul>
     *
     * @param path     文件或目录，文件直接处理
     * @param consumer 文件处理器，只会处理文件
     * @return 文件目录的层级结构
     */
    public static FileNodeDTO getFileNode(String path, Consumer<FileNode> consumer) {

        if (!FileUtil.exist(path)) {
            throw new RuntimeException("文件路径不存在");
        }

        FileNode root = new FileNode();
        FileUtil.walkFiles(
                Paths.get(path),
                new FileVisitor<Path>() {
                    final AtomicInteger depth = new AtomicInteger(0);
                    FileNode fileNode = root;

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        if ("_MACOSX".equals(FileUtil.getName(dir.toFile()))) {
                            return FileVisitResult.SKIP_SIBLINGS;
                        }
                        // 组装
                        FileNode newFileNode = new FileNode();
                        newFileNode.setPath(dir);
                        newFileNode.setAttrs(attrs);
                        newFileNode.setPre(this.fileNode);
                        newFileNode.setName(FileUtil.getName(dir.toFile()));
                        newFileNode.setDepth(depth.getAndIncrement());
                        newFileNode.setIsDirectory(Boolean.TRUE);
                        this.fileNode.addChildNode(newFileNode);
                        this.fileNode = newFileNode;
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        FileNode fileNode = new FileNode();
                        fileNode.setPath(file);
                        fileNode.setAttrs(attrs);
                        fileNode.setPre(this.fileNode);
                        fileNode.setName(FileUtil.getName(file.toFile()));
                        fileNode.setIsDirectory(Boolean.FALSE);
                        fileNode.setDepth(depth.get());

                        this.fileNode.addChildNode(fileNode);
                        // file上传
                        consumer.accept(fileNode);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.TERMINATE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        fileNode = fileNode.getPre();
                        depth.decrementAndGet();
                        return FileVisitResult.CONTINUE;
                    }
                }
        );
        return BeanUtil.copyProperties(root.getChildren().first(), FileNodeDTO.class);
    }

    @Data
    public static class FileNode {

        private static final Comparator<FileNode> DEFAULT_COMPARE_STRATEGY =
                Comparator.<FileNode, Boolean>comparing(x -> !x.getIsDirectory()).thenComparing(x -> x.getName().toLowerCase());

        private Path path;
        private BasicFileAttributes attrs;
        private String name;
        private Boolean isDirectory;
        private Integer depth;
        private FileNode pre;
        private SortedSet<FileNode> children;
        private Map<String, Object> attachments = new HashMap<>();

        public FileNode() {
            children = new TreeSet<>(DEFAULT_COMPARE_STRATEGY);
        }

        public void addChildNode(FileNode fileNode) {
            this.children.add(fileNode);
        }
    }

}
