package cn.x5456.rs.mongo;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yujx
 * @date 2021/05/12 09:37
 */
public class FileLoopTest {

//    @Test
    public void test() throws IOException {

        FileNode root = new FileNode(null, null, null);
        Files.walkFileTree(
                Paths.get("/Users/x5456/Downloads/QQ_DOWNLOADS/5"),
                new FileVisitor<Path>() {

                    FileNode node = root;

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        FileNode newNode = new FileNode(dir, attrs, node);
                        this.node.next.add(newNode);
                        this.node = newNode;
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        this.node.next.add(new FileNode(file, attrs, node));
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.TERMINATE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        node = node.pre;
                        return FileVisitResult.CONTINUE;
                    }
                }
        );

        System.out.println(root.next.get(0));
    }

    class FileNode {
        Path path;
        BasicFileAttributes attrs;

        FileNode pre;

        List<FileNode> next = new ArrayList<>();

        public FileNode(Path path, BasicFileAttributes attrs, FileNode pre) {
            this.path = path;
            this.attrs = attrs;
            this.pre = pre;
        }

        @Override
        public String toString() {
            return "AAA{" +
                    "path=" + path +
                    ", next=" + next +
                    '}';
        }
    }
}
