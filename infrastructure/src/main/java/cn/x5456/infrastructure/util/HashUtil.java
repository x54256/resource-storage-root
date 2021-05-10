package cn.x5456.infrastructure.util;

/**
 * @author: dengdh@dist.com.cn
 * @date: 2021/5/10 16:22
 * @description:
 */
public class HashUtil {
    /**
     * String 自带的 hashCode 函数
     * @param str
     * @return
     */
    public static Integer stringHashCode(String str) {
        int hashCode = str.hashCode();
        if (hashCode < 0) {
            hashCode = Math.abs(hashCode);
        }
        return hashCode;
    }

    /**
     * 使用 FNV1_32_HASH 算法计算 hash 值
     */
    public static Integer FNV1_32_HASH(String str) {
        final int p = 1677769;
        int hash = 0;
        int len = str.length();
        for (int i=0;i<len;i++){
            hash = (hash^str.charAt(i))*p;
        }
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;

        if (hash<0){
            hash=Math.abs(hash);
        }
        return hash;
    }
}
