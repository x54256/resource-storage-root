package cn.x5456.rs.attachment;

import cn.x5456.rs.constant.AttachmentProcessEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yujx
 * @date 2021/05/13 15:07
 */
public class AttachmentProcessContainer {

    private static final Map<AttachmentProcessEnum, AttachmentProcess<?>> attachmentProcessMap = new HashMap<>();

    public static void addAttachmentProcess(AttachmentProcessEnum key, AttachmentProcess<?> process) {
        attachmentProcessMap.put(key, process);
    }

    public static AttachmentProcess<?> getProcess(AttachmentProcessEnum key) {
        return attachmentProcessMap.get(key);
    }
}
