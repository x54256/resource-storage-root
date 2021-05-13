package cn.x5456.rs.constant;

import cn.x5456.infrastructure.enums.EnumInterface;

/**
 * @author yujx
 * @date 2021/05/08 10:01
 */
public enum AttachmentProcessEnum implements EnumInterface {

    COMPRESSED_PACKAGE_PROCESS(0, "fileNode", "压缩包层级结构解析"),
    ;

    int code;
    String key;
    String desc;

    AttachmentProcessEnum(int code, String key, String desc) {
        this.code = code;
        this.key = key;
        this.desc = desc;
    }

    @Override
    public int code() {
        return code;
    }

    public String key() {
        return key;
    }

    @Override
    public String desc() {
        return desc;
    }

    @Override
    public String toString() {
        return key;
    }
}
