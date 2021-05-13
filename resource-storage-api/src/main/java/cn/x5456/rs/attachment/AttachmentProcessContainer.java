package cn.x5456.rs.attachment;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yujx
 * @date 2021/05/13 15:07
 */
public final class AttachmentProcessContainer {

    private static final Map<String, AttachmentProcess<?>> attachmentProcessMap = new HashMap<>();

    public static void addProcess(String key, AttachmentProcess<?> process) {
        attachmentProcessMap.put(key, process);
    }

    public static AttachmentProcess<?> getProcess(String key) {
        return attachmentProcessMap.get(key);
    }

    private AttachmentProcessContainer() {
    }
}
