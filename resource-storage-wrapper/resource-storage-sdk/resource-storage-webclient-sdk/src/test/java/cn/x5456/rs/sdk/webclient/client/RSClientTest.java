package cn.x5456.rs.sdk.webclient.client;

/**
 * @author yujx
 * @date 2021/05/17 09:56
 */
//@ActiveProfiles("nacos")   // maven 的 profile 好像在单元测试中不好使
//@Slf4j
//@SpringBootTest(classes = BootstrapConfig.class)
//@RunWith(SpringRunner.class)
//public class RSClientTest {
//
//    @Autowired
//    private RSClient client;
//
//    @Test
//    public void downloadV2() {
//        ResourceInfo resourceInfo = client.uploadFile(new File(FileUtil.getAbsolutePath("1.txt"))).block();
//        log.info("resourceInfo：「{}」", resourceInfo);
//        ClientResponse response = client.downloadV2(resourceInfo.getId(), HttpRange.createByteRange(0, 3)).block();
//        Assert.assertEquals(4L, response.headers().contentLength().getAsLong());
//    }
//}