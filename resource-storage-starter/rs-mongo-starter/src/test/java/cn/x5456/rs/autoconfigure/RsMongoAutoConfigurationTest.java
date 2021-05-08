package cn.x5456.rs.autoconfigure;

import cn.x5456.rs.def.IResourceStorage;
import cn.x5456.rs.def.block.IBlockResourceStorage;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author yujx
 * @date 2021/05/08 11:10
 */
@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@SpringBootApplication
public class RsMongoAutoConfigurationTest {

    @Autowired
    private IResourceStorage resourceStorage;

    @Autowired
    private IBlockResourceStorage blockResourceStorage;

    @Test
    public void test() {
        log.info("resourceStorage：「{}」", resourceStorage);
        log.info("blockResourceStorage：「{}」", blockResourceStorage);
    }
}