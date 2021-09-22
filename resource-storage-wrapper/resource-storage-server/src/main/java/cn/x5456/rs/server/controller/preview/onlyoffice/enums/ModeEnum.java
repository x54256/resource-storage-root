package cn.x5456.rs.server.controller.preview.onlyoffice.enums;

import cn.x5456.infrastructure.enums.EnumInterface;

/**
 * @author yujx
 * @date 2021/09/17 15:29
 */
public enum ModeEnum implements EnumInterface {

    /**
     * 只读
     */
    VIEW(0, "view"),

    /**
     * 编辑
     */
    EDIT(1, "edit");

    private final int code;
    private final String desc;

    ModeEnum(int code, String desc) {
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