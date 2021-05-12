package cn.x5456.infrastructure.util;

import cn.hutool.core.io.FileUtil;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        if (!FileUtil.exist(path)) {
            throw new RuntimeException("文件路径不存在");
        }

        FileNode root = new FileNode();
        FileUtil.walkFiles(
                Paths.get(path),
                new FileVisitor<Path>() {
                    AtomicInteger depth = new AtomicInteger(0);
                    FileNode fileNode = root;

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        // 组装
                        FileNode newFileNode = new FileNode();
                        newFileNode.setPath(dir);
                        newFileNode.setAttrs(attrs);
                        newFileNode.setPre(this.fileNode);
                        newFileNode.setName(FileUtil.getName(dir.toFile()));
                        newFileNode.setDepth(depth.getAndIncrement());
                        newFileNode.setDirectory(Boolean.TRUE);
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
                        fileNode.setDirectory(Boolean.FALSE);
                        fileNode.setDepth(depth.get());

                        this.fileNode.addChildNode(fileNode);
                        // file上set
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
        return root.getChildren().first();
    }

}
