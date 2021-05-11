package cn.x5456.rs.mongo;

import cn.hutool.system.oshi.OshiUtil;
import org.junit.Test;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;

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

            System.out.println(diskStore.getSize());
            System.out.println(diskStore.getWriteBytes());
            System.out.println((double) diskStore.getWriteBytes() / (double) diskStore.getSize());
            for (HWPartition partition : diskStore.getPartitions()) {
                System.out.println("partition = " + partition);
            }
        }
    }
}
