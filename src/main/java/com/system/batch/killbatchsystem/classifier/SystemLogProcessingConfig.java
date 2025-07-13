package com.system.batch.killbatchsystem.classifier;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ClassifierCompositeItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.classify.Classifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;

@Slf4j
//@Configuration
@RequiredArgsConstructor
public class SystemLogProcessingConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job systemLogProcessingJob() {
        return new JobBuilder("systemLogProcessingJob", jobRepository)
                .start(systemLogProcessingStep())
                .build();
    }

    @Bean
    public Step systemLogProcessingStep() {
        return new StepBuilder("systemLogProcessingStep", jobRepository)
                .<SystemLog, SystemLog>chunk(10, transactionManager)
                .reader(systemLogProcessingReader())
                .writer(classifierWriter())
                .build();
    }

    @Bean
    public ListItemReader<SystemLog> systemLogProcessingReader() {
        List<SystemLog> logs = new ArrayList<>();
        // 테스트용 데이터 생성
        SystemLog criticalLog = new SystemLog();
        criticalLog.setType("CRITICAL");
        criticalLog.setMessage("OOM 발생!! 메모리가 바닥났다!");
        criticalLog.setCpuUsage(95);
        criticalLog.setMemoryUsage(2024 * 1024 * 1024L);
        logs.add(criticalLog);

        SystemLog normalLog = new SystemLog();
        normalLog.setType("NORMAL");
        normalLog.setMessage("시스템 정상 작동 중");
        normalLog.setCpuUsage(30);
        normalLog.setMemoryUsage(512 * 1024 * 1024L);
        logs.add(normalLog);

        return new ListItemReader<>(logs);
    }

    @Bean
    public ClassifierCompositeItemWriter<SystemLog> classifierWriter() {
        ClassifierCompositeItemWriter<SystemLog> writer = new ClassifierCompositeItemWriter<>();
        writer.setClassifier(new SystemLogClassifier(criticalLogWriter(), normalLogWriter()));
        return writer;
    }

    @Bean
    public ItemWriter<SystemLog> normalLogWriter() {
        return items -> {
            log.info("✅NoramLogWriter: 일반 로그 처리 중... 대충 파일에 출력하거나 하자..");
            for (SystemLog item : items) {
                log.info("✅일반 처리: {}", item);
            }
        };
    }

    @Bean
    public ItemWriter<SystemLog> criticalLogWriter() {
        return items -> {
            log.info("🚨CriticalLogWriter: 치명적 시스템 로그 감지! 즉시 처리 시작!");
            for (SystemLog item : items) {
                // 실제 운영에선 여기서 슬랙 혹은 이메일 발송
                log.info("🚨긴급 처리: {}", item);
            }
        };
    }

    public static class SystemLogClassifier implements Classifier<SystemLog, ItemWriter<? super SystemLog>> {
        public static final int CRITICAL_CPU_THRESHOLD = 90;
        public static final long CRITICAL_MEMORY_THRESHOLD = 1024 * 1024 * 1024; // 1GB

        private final ItemWriter<SystemLog> criticalWriter;
        private final ItemWriter<SystemLog> normalWriter;

        public SystemLogClassifier(
                ItemWriter<SystemLog> criticalWriter,
                ItemWriter<SystemLog> normalWriter) {
            this.criticalWriter = criticalWriter;
            this.normalWriter = normalWriter;
        }

        @Override
        public ItemWriter<SystemLog> classify(SystemLog log) {
            if (isCritical(log)) {
                return criticalWriter;
            }
            return normalWriter;
        }

        // 시스템의 생사를 가르는 판단 기준
        private boolean isCritical(SystemLog log) {
            return "CRITICAL".equals(log.getType()) ||
                    log.getCpuUsage() >= CRITICAL_CPU_THRESHOLD ||
                    log.getMemoryUsage() >= CRITICAL_MEMORY_THRESHOLD;
        }
    }

    @Data
    public static class SystemLog {
        private String type;      // CRITICAL or NORMAL
        private String message;
        private int cpuUsage;
        private long memoryUsage;
    }
}
