package com.system.batch.killbatchsystem.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.redis.RedisItemWriter;
import org.springframework.batch.item.redis.builder.RedisItemWriterBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;

//@Configuration
@RequiredArgsConstructor
public class AttackerBlockListJob {
    private final RedisConnectionFactory redisConnectionFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job registerAttackerBlockListJob() {
        return new JobBuilder("registerAttackerBlockListJob", jobRepository)
                .start(registerAttackerBlockListStep())
                .build();
    }

    @Bean
    public Step registerAttackerBlockListStep() {
        return new StepBuilder("registerAttackerBlockListStep", jobRepository)
                .<AttackerInfo, AttackerInfo>chunk(10, transactionManager)
                .reader(attackerInfoReader())
                .writer(attackerInfoWriter())
                .build();
    }

    @Bean
    public ListItemReader<AttackerInfo> attackerInfoReader() {
        return new ListItemReader<>(List.of(
                new AttackerInfo("1234", "SQL Injection", "CRITICAL", "서버실 침투 시도", "192.168.0.100", LocalDateTime.now()),
                new AttackerInfo("5678", "DDoS", "HIGH", "대규모 트래픽 공격", "10.0.0.50", LocalDateTime.now()),
                new AttackerInfo("9012", "XSS", "MEDIUM", "쿠키 탈취 시도", "172.16.0.1", LocalDateTime.now())
        ));
    }

    @Bean
    public RedisItemWriter<String, AttackerInfo> attackerInfoWriter() {
        // JavaTimeModule 지원을 위한 ObjectMapper 설정
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RedisTemplate<String, AttackerInfo> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, AttackerInfo.class));
        template.afterPropertiesSet();

        return new RedisItemWriterBuilder<String, AttackerInfo>()
                .redisTemplate(template)
                .itemKeyMapper(attackerInfo -> "attacker:" + attackerInfo.getId())
                .delete(false)
                .build();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttackerInfo {
        private String id;
        private String originalAttack;
        private String threatLevel;
        private String description;
        private String lastKnownIp;
        private LocalDateTime detectedAt;
    }
}