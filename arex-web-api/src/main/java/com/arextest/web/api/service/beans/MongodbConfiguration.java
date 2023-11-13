package com.arextest.web.api.service.beans;

import com.arextest.web.common.LogUtils;
import com.arextest.web.model.dao.mongodb.AppContractCollection;
import com.arextest.web.model.dao.mongodb.LogsCollection;
import com.arextest.web.model.dao.mongodb.ReplayScheduleConfigCollection;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Slf4j
@Configuration
public class MongodbConfiguration {

  private static final String APP_ID = "appId";
  private static final String SERVICE_ID = "serviceId";
  private static final String OPERATION_NAME = "operationName";
  private static final String DATE = "date";
  private static final String DATE_UPDATE_TIME = "dataUpdateTime";
  private static final String AREX_STORAGE_DB = "arex_storage_db";
  @Value("${arex.mongo.uri}")
  private String mongoUrl;

  public MongoDatabaseFactory mongoDbFactory() {
    try {
      ConnectionString connectionString = new ConnectionString(mongoUrl);
      String dbName = connectionString.getDatabase();
      if (dbName == null) {
        dbName = AREX_STORAGE_DB;
      }

      CodecRegistry pojoCodecRegistry =
          CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
              CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));

      MongoClientSettings settings = MongoClientSettings.builder()
          .applyConnectionString(connectionString)
          .codecRegistry(pojoCodecRegistry).build();
      MongoClient mongoClient = MongoClients.create(settings);
      return new SimpleMongoClientDatabaseFactory(mongoClient, dbName);
    } catch (Exception e) {
      LogUtils.error(LOGGER, "cannot connect mongodb", e);
    }
    return null;
  }

  @Lazy
  @Bean
  @ConditionalOnMissingBean
  public MongoTemplate mongoTemplate() {
    DbRefResolver dbRefResolver = new DefaultDbRefResolver(this.mongoDbFactory());
    MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver,
        new MongoMappingContext());
    converter.setTypeMapper(new DefaultMongoTypeMapper(null));
    MongoTemplate template = new MongoTemplate(this.mongoDbFactory(), converter);
    initIndicesAfterStartup(template);
    return template;
  }

  public void initIndicesAfterStartup(MongoTemplate mongoTemplate) {

    // indexs for ReplayScheduleConfigCollection
    mongoTemplate.indexOps(ReplayScheduleConfigCollection.class)
        .ensureIndex(new Index().on(APP_ID, Sort.Direction.ASC).unique());

    // indexs for LogsCollection
    mongoTemplate.indexOps(LogsCollection.class)
        .ensureIndex(new Index(DATE, Sort.Direction.DESC).expire(10, TimeUnit.DAYS));

    // indexs for AppContractCollection
    mongoTemplate.indexOps(AppContractCollection.class)
        .ensureIndex(new Index().on(AppContractCollection.Fields.appId, Sort.Direction.ASC));
    mongoTemplate.indexOps(AppContractCollection.class)
        .ensureIndex(new Index().on(AppContractCollection.Fields.operationId, Sort.Direction.ASC));

  }
}
