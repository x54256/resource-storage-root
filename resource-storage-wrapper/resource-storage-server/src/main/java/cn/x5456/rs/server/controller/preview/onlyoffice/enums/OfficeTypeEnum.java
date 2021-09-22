package cn.x5456.rs.server.controller.preview.onlyoffice.enums;

import cn.x5456.infrastructure.enums.EnumInterface;

/**
 * @author yujx
 * @date 2021/09/17 15:29
 */
public enum OfficeTypeEnum implements EnumInterface {

    /**
     * 文本文件格式
     */
    TEXT(0, "text"),

    /**
     * Excel 文件格式
     */
    SPREADSHEET(1, "spreadsheet"),

    /**
     * PPT 文件格式
     */
    PRESENTATION(2, "presentation");

    private final int code;
    private final String desc;

    OfficeTypeEnum(int code, String desc) {
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