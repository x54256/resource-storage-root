package cn.x5456.infrastructure.enums;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;

/**
 * @author yujx
 * @date 2021/03/19 16:03
 */
public interface EnumInterface {

    int code();

    String desc();

    static <T extends Enum<T> & EnumInterface> T get(Class<T> tClass, int code) {
        T[] enumConstants = tClass.getEnumConstants();
        if (ArrayUtil.isEmpty(enumConstants)) {
            throw new RuntimeException(StrUtil.format("枚举「{}」没有枚举对象", tClass.getSimpleName()));
        }
        for (T enumConstant : enumConstants) {
            if (enumConstant.code() == code) {
                return enumConstant;
            }
        }
        return null;
    }

    static <T extends Enum<T> & EnumInterface> T getByDesc(Class<T> tClass, String desc) {
        T[] enumConstants = tClass.getEnumConstants();
        if (ArrayUtil.isEmpty(enumConstants)) {
            throw new RuntimeException(StrUtil.format("枚举「{}」没有枚举对象", tClass.getSimpleName()));
        }
        for (T enumConstant : enumConstants) {
            if (enumConstant.desc().equals(desc)) {
                return enumConstant;
            }
        }
        return null;
    }

    static <T extends Enum<T> & EnumInterface> boolean isLegal(Class<T> tClass, int code) {
        return get(tClass, code) != null;
    }

}