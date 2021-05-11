package cn.x5456.rs.mongo;

import cn.hutool.system.oshi.OshiUtil;
import org.junit.Test;
import oshi.hardware.HWDiskStore;

import java.util.List;

/**
 * @author yujx
 * @date 2021/05/11 12:17
 */
public class OshiTest {

    @Test
    public void test() {
        List<HWDiskStore> diskStores = OshiUtil.getDiskStores();
        for (HWDiskStore diskStore : diskStores) {
            System.out.println("diskStore = " + diskStore);
        }
    }
}
