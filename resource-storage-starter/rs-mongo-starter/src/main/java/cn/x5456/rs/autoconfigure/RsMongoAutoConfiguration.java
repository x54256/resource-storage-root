package cn.x5456.rs.autoconfigure;

import cn.x5456.rs.def.IResourceStorage;
import cn.x5456.rs.def.block.IBlockResourceStorage;
import cn.x5456.rs.def.block.ResourceStorageBlockWrapper;
import cn.x5456.rs.mongo.MongoResourceStorage;
import cn.x5456.rs.mongo.cleanstrategy.MongoSocketTimeoutHolder;
import com.mongodb.reactivestreams.client.MongoClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import reactor.core.scheduler.Scheduler;

/**
 * @author yujx
 * @date 2021/05/08 11:01
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({MongoClient.class, ReactiveMongoTemplate.class})
@ConditionalOnBean({ReactiveMongoTemplate.class, DefaultDataBufferFactory.class, ReactiveGridFsTemplate.class})
@AutoConfigureAfter(MongoReactiveDataAutoConfiguration.class)
public class RsMongoAutoConfiguration {

    @Bean
    public IResourceStorage mongoResourceStorage(DataBufferFactory dataBufferFactory, ReactiveMongoTemplate mongoTemplate,
                                                 ReactiveGridFsTemplate gridFsTemplate, ObjectProvider<Scheduler> schedulerObjectProvider) {
        return new MongoResourceStorage(dataBufferFactory, mongoTemplate, gridFsTemplate, schedulerObjectProvider);
    }

    @Bean
    public IBlockResourceStorage blockMongoResourceStorage(DataBufferFactory dataBufferFactory, IResourceStorage resourceStorage) {
        return new ResourceStorageBlockWrapper(dataBufferFactory, resourceStorage);
    }

    @Bean
    public MongoSocketTimeoutHolder mongoSocketTimeoutHolder() {
        return new MongoSocketTimeoutHolder();
    }

}
