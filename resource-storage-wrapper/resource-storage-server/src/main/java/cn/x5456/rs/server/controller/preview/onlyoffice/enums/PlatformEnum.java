package cn.x5456.rs.server.controller.preview.onlyoffice.enums;

import cn.x5456.infrastructure.enums.EnumInterface;

/**
 * @author yujx
 * @date 2021/09/17 15:29
 */
public enum PlatformEnum implements EnumInterface {

    /**
     * 桌面端
     */
    DESKTOP(0, "desktop"),

    /**
     * 移动端
     */
    MOBILE(1, "mobile"),

    /**
     * 嵌入式
     */
    EMBEDDED(2, "embedded");

    private final int code;
    private final String desc;

    PlatformEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String desc() {
        return desc;
    }
}
