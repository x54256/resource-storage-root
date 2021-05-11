package cn.x5456.rs.mongo;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.junit.Test;

/**
 * @author yujx
 * @date 2021/05/11 13:19
 */
public class BloomFilterTest {

    private int size = 1000000;
    private final BloomFilter<Integer> bloomFilter = BloomFilter.create(
            // Funnel 预估元素个数 误判率
            Funnels.integerFunnel(), size, 0.01);


    @Test
    public void test() {
        for (int i = 0; i < size; i++) {
            bloomFilter.put(i);
        }

//        for (int i = size; i < size + 100000; i++) {
//            if (bloomFilter.mightContain(i)) {
//                System.out.println(i + "误判了！");
//            }
//        }

        // 误判会一直误判，最好在初始化的时候确定好元素的数量
        // ![](https://tva1.sinaimg.cn/large/008i3skNgy1gqefvybxobj314w0k6q96.jpg)
        for (int i = 0; i < 100; i++) {
            System.out.println(bloomFilter.mightContain(1000113));
        }
    }
}
