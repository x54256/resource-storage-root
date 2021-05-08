# rs

1. nio 线程不能被阻塞（死锁问题）
2. mongo 的唯一索引是采用乐观锁机制做的所以不可靠，如果需要可靠的唯一索引机制最好计算出一个唯一 hash 值来作为主键

![](https://tva1.sinaimg.cn/large/008i3skNgy1gq8tqbytahj316n0u0wn1.jpg)

![](https://tva1.sinaimg.cn/large/008i3skNgy1gq8tpzjk3kj31gs0rsqan.jpg)


3. 当前面操作返回的是一个空的 Publisher map() 是拿不到数据的，所以不能通过 .map(x -> if(x == null)) 来做一些事情，要使用 switchIfEmpty() 来进一步操作
4. 如果代码不涉及 scheduler 最好不要使用 sink，flatMap 和 map 可以胜任绝大部分任务。

问题：本地文件没就绪怎么办？  ->  下载完成后更名  .tmp -> .xxx


计算 hash 时间太长怎么办？

小文件耗时不会太多。大文件前端计算。

当返回值为空的时候，使用 map() 拿不到元素，要使用 switchIfEmpty() 来进一步操作

**nio 线程不能被阻塞**，如果需要阻塞，请在调用时创建一个新线程

```java
class A {

    @NotNull
    private Mono<String> download(String fileHash) {
        return Mono.create(sink -> {
            this.getReadyMetadata(fileHash)
                    .subscribe(m -> {
                        // 拼接本地缓存路径，格式：缓存目录/hashcode.tmp
                        String tempPath = LOCAL_TEMP_PATH + m.getFileHash() + SUFFIX;
                        if (FileUtil.exist(tempPath)) {
                            sink.success(tempPath);
                        } else {
                            log.info("tempPath：「{}」", tempPath);
                            // 2021/4/28 why????? 为啥要新开一个线程
                            // 假设当前代码运行的线程为 N2-2，我们进入 doDownload() 方法，里面有一个循环，也是使用 N2-2 线程发送两个请求
                            // （应该是做了判断，判断当前线程是不是 EventLoopGroup 中的线程，如果不是才会进行线程的切换），可能 mongo 内部
                            // 有一个机制就是请求线程与接收线程绑定，即第一个请求用 N2-2 接收，第二个请求用 N2-3 接收，因为我们 for 循环之后
                            // 调用了 latch.await(); 将 N2-2 阻塞住了，所以当消息来了之后 N2-2 无法接收，所以程序一直无法停止。
                            // 所以，我们不能让 nio 线程阻塞，那就需要在调用时重新创建一个线程了。
                            scheduler.schedule(() -> {
                                // 这块已经是子线程了，但还是最好不要用阻塞 api 吧
                                String block = this.doDownload(m, tempPath).block();
                                sink.success(tempPath);
                            });
                        }
                    });
        });
    }

    @NotNull
    private Mono<String> doDownload(FileMetadata metadata, String tempPath) {
        return Mono.create(sink -> {
            // 获取每一片的信息，排序
            List<FileMetadata.FsFilesInfo> fsFilesInfoList = metadata.getFsFilesInfoList();
            fsFilesInfoList.sort(Comparator.comparingInt(FileMetadata.FsFilesInfo::getChunk));

            long index = 0;
            CountDownLatch latch = new CountDownLatch(fsFilesInfoList.size());
            try {
                for (FileMetadata.FsFilesInfo fsFilesInfo : fsFilesInfoList) {
                    RandomAccessFile randomAccessFile = new RandomAccessFile(tempPath, "rw");
                    randomAccessFile.seek(index);
                    index += fsFilesInfo.getChunkSize();

                    // 开始下载
                    gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(fsFilesInfo.getFsFilesId())))
                            .log()
                            .flatMap(gridFsTemplate::getResource)
                            .map(ReactiveGridFsResource::getDownloadStream)
                            .flux()
                            .flatMap(dataBufferFlux -> DataBufferUtils.write(dataBufferFlux, randomAccessFile.getChannel()))
                            .doOnError(sink::error)
                            .doOnComplete(() -> {
                                try {
                                    randomAccessFile.close();
                                } catch (IOException e) {
                                    sink.error(e);
                                }
                                latch.countDown();
                            }).subscribe();
                }

                // 注释掉，在调用方加个 sleep 就可以看到正常接收了 onNext()
                latch.await();
                sink.success(tempPath);
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }
}
```

方案一：定时任务版（一次任务的时间间隔不好控制，每次间隔随机时间看看好不好实现），不用结合 AbstractMongoEventListener

当保存之后调用回调，创建一个延迟任务，检查这个文件是否上传完成，如果还没上传完就删除； -> 感觉没必要
而且，假如当前服务器挂了，那么这个文件的元数据永远不会删，所以还是要用定时任务。

定时任务就很简单了，定时去 metadata 表查询还没有上传完成并且时间超过了 mongo 超时时间两倍（这个参数可以通过这个 listener 设置）的进行删除。


方案二：redis 过期键版（发布-订阅），结合 AbstractMongoEventListener

需要引入 spring-data-redis，当保存之后调用回调，创建一个具有过期时间的 key，过期后调用回调进行删除
问题：是否所有实例都会收到这个监听呢。

v1 版本模仿 spring-redis-session 的 A、B、C 类型键，但是写着写着发现 redis 好像给我们实现了一个

v2 通过 @EnableRedisRepositories 用二级索引实现

![](https://tva1.sinaimg.cn/large/008i3skNgy1gq9nru2s7dj318y0aadjb.jpg)

问题：Redis Pub / Sub消息不是持久的。 如果在应用程序关闭期间某个键过期，则不会处理到期事件，这可能会导致secondary indexes引用已过期的对象。

![](https://tva1.sinaimg.cn/large/008i3skNgy1gq9oa059srj30qg0a276c.jpg)

方案三：MQ 版（最好，但一般项目不会用 mq），结合 AbstractMongoEventListener

和 redis 版本大同小异，可以交给陆俊翔写。

适配器模式呗 或 条件配置类（感觉这个靠谱）

---

遗留技术问题：

1. mongo id 加索引和唯一索引有啥区别
2. 如果在应用程序关闭期间某个键过期，则不会处理到期事件，这可能会导致secondary indexes引用已过期的对象。
    - 二级索引学习
3. ~~布隆过滤器改造~~ -> 做不到
4. 了解下 guava 布隆过滤器，超出的元素是否会删除

剩余功能点：

- zip 解压，内部文件上传 mongo
- mongo 那些注解好不好使 {@link com.mongodb.lang.NonNull}
- ~~布隆过滤器线程安全问题 -> 自己学习下 ConcurrentHashMap 封装一下~~
- 你觉得清理策略自动启用好还是指定好，如果自动启用，有什么办法调整那两种策略的优先级吗，因为我只想启用一个
- 作为 jar 包引入，提供 rest api（包括 swagger） 和钩子（hook）
    1. 文件服务独立部署，通过网关转发
    2. 通过 jar 包引入文件服务，最好提供 webflux api
- 分布式情况下，结合网关使用
- 分布式独立部署情况下使用 hash 环算法负载
- 使用了 hash 环就可以在本地合并了。-> 0. 构造的时候加一个属性，是否需要本地合并该文件
- 问题：
    ```
    第一个请求在上传 hash 值为 123abc 的文件，此时他在 metadata 表的状态是上传中。
    第二个请求也上传 hash 值为 123abc 的文件，因为 metadata 表中已经存在了，所以在 resource 表建立了引用，实现了秒传。
    但是第一个请求上传失败了，metadata 表数据删除了，怎么办！
    
    1. 如果发现已经有了一个 hash 值为 123abc 的文件在上传中，则阻塞住，等他上传完之后再建立引用。
    2. 新增一张 hash 冲突表，也进行上传，上传完成之后，如果：
        1）第一个请求上传成功，删除我们上传的文件（异步），建立引用
        2）如果第一个请求还在上传中状态，怎么办呢~~ -> 阻塞住，等待自动清理完成之后达成 3）的条件
            todo 测试下，同一个应用 t1 轮训，t2 删除是否能感知到。
        3）如果数据没了，直接把我们的转正到 metadata 表中，建立引用
    
        4）如果我们上传中，第一个请求上传完成了怎么办？
    ```