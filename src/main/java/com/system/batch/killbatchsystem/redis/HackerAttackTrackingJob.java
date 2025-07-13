package com.system.batch.killbatchsystem.redis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.redis.RedisItemReader;
import org.springframework.batch.item.redis.builder.RedisItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
//@Configuration
@RequiredArgsConstructor
public class HackerAttackTrackingJob {
    private final RedisConnectionFactory redisConnectionFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job trackHackerAttackJob() {
        return new JobBuilder("trackHackerAttackJob", jobRepository)
                .start(trackHackerAttackStep())
                .build();
    }

    @Bean
    public Step trackHackerAttackStep() {
        return new StepBuilder("trackHackerAttackStep", jobRepository)
                .<AttackLog, AttackLog>chunk(10, transactionManager)
                .reader(attackLogReader())
                .writer(items -> items.forEach(attackLog ->
                        log.info("[공격 감지] {}", attackLog)))
                .build();
    }

    @Bean
    public RedisItemReader<String, AttackLog> attackLogReader() {
        RedisTemplate<String, AttackLog> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(AttackLog.class));
        template.afterPropertiesSet();

        return new RedisItemReaderBuilder<String, AttackLog>()
                .redisTemplate(template)
                .scanOptions(ScanOptions.scanOptions()
                        .match("attack:*")  // attack: 으로 시작하는 키만 스캔
                        .count(100)         // 한 번에 100개씩 스캔
                        .build())
                .build();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttackLog {
        private String timestamp;
        private String targetIp;
        private String attackType;
        private String payload;
    }
}