package com.system.batch.killbatchsystem.partitional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.batch.item.redis.RedisItemReader;
import org.springframework.batch.item.redis.builder.RedisItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BattlefieldLogPersistenceJobConfig {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final RedisConnectionFactory redisConnectionFactory;
    private final MongoTemplate mongoTemplate;
    private final DailyTimeRangePartitioner dailyTimeRangePartitioner;

    @Bean
    public Job battlefieldLogPersistenceJob(Step managerStep) {
        return new JobBuilder("battlefieldLogPersistenceJob", jobRepository)
                .start(managerStep)
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    public Step managerStep(Step workerStep) {
        return new StepBuilder("managerStep", jobRepository)
                .partitioner("workerStep", dailyTimeRangePartitioner)
                .step(workerStep)
                .taskExecutor(partitionTaskExecutor())
                .gridSize(4) // 💀 24시간을 4개(6시간)의 파티션으로 분할 💀

                .build();
    }

    @Bean
    public Step workerStep(
            RedisItemReader<String, BattlefieldLog> redisLogReader,
            ItemProcessor<BattlefieldLog, BattlefieldLog> logProcessor,
            MongoItemWriter<BattlefieldLog> mongoLogWriter
    ) {
        return new StepBuilder("workerStep", jobRepository)
                .<BattlefieldLog, BattlefieldLog>chunk(500, transactionManager)
                .reader(redisLogReader)
                .processor(logProcessor)
                .writer(mongoLogWriter)
                .build();
    }

    @Bean
    @StepScope
    public RedisItemReader<String, BattlefieldLog> redisLogReader(
            @Value("#{stepExecutionContext['startDateTime']}") LocalDateTime startDateTime) {
        return new RedisItemReaderBuilder<String, BattlefieldLog>()
                .redisTemplate(redisTemplate())
                .scanOptions(ScanOptions.scanOptions()
                        .match("logs:" + startDateTime.format(FORMATTER) + ":*")
                        .count(10000)
                        .build())
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<BattlefieldLog, BattlefieldLog> logProcessor() {
        return battlefieldLog -> {
            log.info("Thread: {} - Processing log ID: {}, ",
                    Thread.currentThread().getName(),
                    battlefieldLog.getId());
            return battlefieldLog;
        };
    }

    @Bean
    @StepScope
    public MongoItemWriter<BattlefieldLog> mongoLogWriter() {
        return new MongoItemWriterBuilder<BattlefieldLog>()
                .template(mongoTemplate)
                .mode(MongoItemWriter.Mode.INSERT)
                .build();
    }

    @Bean
    public TaskExecutor partitionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 💀 파티션 개수(gridSize)와 스레드풀 크기를 일치시키면 각 파티션이 💀
        // 💀 전용 스레드를 할당받아 대기 시간 없이 즉시 처리될 수 있다. 💀
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setThreadNamePrefix("Partition-thread-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }

    @Bean
    public RedisTemplate<String, BattlefieldLog> redisTemplate() {
        RedisTemplate<String, BattlefieldLog> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(mapper, BattlefieldLog.class));
        return redisTemplate;
    }
}
