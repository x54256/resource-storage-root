package cn.x5456.rs.mongo;

import cn.x5456.rs.def.IResourceStorage;
import cn.x5456.rs.def.block.IBlockResourceStorage;
import cn.x5456.rs.def.block.ResourceStorageBlockWrapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import reactor.core.scheduler.Scheduler;

@Configuration
@EnableAutoConfiguration
public class MongoConfiguration {

    @Bean
    public IResourceStorage mongoResourceStorage(DataBufferFactory dataBufferFactory, ReactiveMongoTemplate mongoTemplate,
                                                 ReactiveGridFsTemplate gridFsTemplate, ApplicationEventPublisher eventPublisher,
                                                 ObjectProvider<Scheduler> schedulerObjectProvider) {
        return new MongoResourceStorage(dataBufferFactory, mongoTemplate, gridFsTemplate, eventPublisher, schedulerObjectProvider);
    }

    @Bean
    public IBlockResourceStorage blockMongoResourceStorage(DataBufferFactory dataBufferFactory, IResourceStorage resourceStorage) {
        return new ResourceStorageBlockWrapper(dataBufferFactory, resourceStorage);
    }

}
