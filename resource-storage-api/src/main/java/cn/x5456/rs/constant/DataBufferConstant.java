package cn.x5456.rs.constant;

/**
 * @author yujx
 * @date 2021/05/08 10:01
 */
public final class DataBufferConstant {

    /**
     * 256 KB
     * {@see org.springframework.data.mongodb.gridfs.ReactiveGridFsResource.DEFAULT_CHUNK_SIZE}
     */
    public static final Integer DEFAULT_CHUNK_SIZE = 256 * 1024;

    private DataBufferConstant() {
    }
}
