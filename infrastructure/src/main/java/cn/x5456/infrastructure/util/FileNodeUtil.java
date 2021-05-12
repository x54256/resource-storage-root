package cn.x5456.infrastructure.util;

import cn.hutool.core.io.FileUtil;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: dengdh@dist.com.cn
 * @date: 2021/5/12 14:27
 * @description:
 */
public final class FileNodeUtil {

    private FileNodeUtil() {
    }

    public static FileNode getFileNode(String path) {
        FileNode root = FileNode.builder().build();
        FileUtil.walkFiles(
                Paths.get(path),
                new FileVisitor<Path>() {
                    AtomicInteger depth = new AtomicInteger(0);
                    FileNode fileNode = root;

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        // 组装
                        FileNode newFileNode = FileNode.builder()
                                .path(dir)
                                .attrs(attrs)
                                .pre(this.fileNode)
                                .name(FileUtil.getName(dir.toFile()))
                                .depth(depth.getAndIncrement())
                                .isDirectory(Boolean.TRUE).build();
                        this.fileNode.addChildNode(newFileNode);
                        this.fileNode = newFileNode;
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        this.fileNode.addChildNode(FileNode.builder()
                                .path(file)
                                .attrs(attrs)
                                .pre(fileNode)
                                .name(FileUtil.getName(file.toFile()))
                                .isDirectory(Boolean.FALSE)
                                .depth(depth.get())
                                .build());
                        // file上传
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
        return root;
    }

    public static void main(String[] args) throws IOException {
        FileNodeUtil.getFileNode("F:\\MyWork\\文件存储\\resource-storage-root");
    }
}
