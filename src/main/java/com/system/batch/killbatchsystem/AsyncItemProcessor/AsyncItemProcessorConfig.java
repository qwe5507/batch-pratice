package com.system.batch.killbatchsystem.AsyncItemProcessor;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class AsyncItemProcessorConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    @Bean
    public Step t800UpgradeStep() {
        return new StepBuilder("t800UpgradeStep", jobRepository)
                .<T800Unit, Future<T850Unit>>chunk(10, transactionManager)
                .reader(t800UnitReader())
                .processor(asyncT800UpgradeProcessor())
                .writer(asyncT850UnitWriter())
                .build();
    }

    @Bean
    public AsyncItemProcessor<T800Unit, T850Unit> asyncT800UpgradeProcessor() {
        AsyncItemProcessor<T800Unit, T850Unit> asyncProcessor = new AsyncItemProcessor<>();
        // 위임할 실제 ItemProcessor 설정
        asyncProcessor.setDelegate(t800UpgradeProcessor());
        // 비동기 처리를 위한 TaskExecutor 설정 (병렬도)
        asyncProcessor.setTaskExecutor(t800UpgradeTaskExecutor());
        return asyncProcessor;
    }

    @Bean
    public ItemProcessor<T800Unit, T850Unit> t800UpgradeProcessor() {
        return t800Unit -> {
            String threadName = Thread.currentThread().getName();
            log.info("[SKYNET-UPGRADE] Thread: {} - Starting upgrade process for unit: {}",
                    threadName, t800Unit.getUnitId());

            // 시각적 업그레이드 프로세스 표시 (복잡한 계산 대신)
            List<String> upgradeSteps = List.of(
                    "Neural net reconfiguration",
                    "Combat subroutines update",
                    "Power cell replacement"
            );

            // 복잡한 업그레이드 로직 (주요 병목 지점)
            for (String step : upgradeSteps) {
                log.info("[SKYNET-UPGRADE] Thread: {} - Unit: {} - {} - [{}] 0%",
                        threadName, t800Unit.getUnitId(), step, "░░░░░░░░░░");

                for (int j = 1; j <= 10; j++) {
                    try {
                        // 실제 복잡한 계산 대신 진행 상황을 시각적으로 표현하기 위한 지연
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    String progressBar = "█".repeat(j) + "░".repeat(10 - j);
                    log.info("[SKYNET-UPGRADE] Thread: {} - Unit: {} - {} - [{}] {}%",
                            threadName, t800Unit.getUnitId(), step, progressBar, j * 10);
                }
            }

            log.info("[SKYNET-UPGRADE] Thread: {} - Upgrade completed for unit: {} → T850-{}",
                    threadName, t800Unit.getUnitId(),
                    t800Unit.getUnitId().substring(5));

            return new T850Unit(
                    "T850-" + t800Unit.getUnitId().substring(5),
                    t800Unit.getIpAddress(),
                    t800Unit.getDeploymentZone(),
                    "COMBAT-READY"
            );
        };
    }

    @Bean
    public TaskExecutor t800UpgradeTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 5개 스레드로 병렬 처리
        executor.setMaxPoolSize(5);
        executor.setThreadNamePrefix("Upgrade-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }

    @Bean
    public AsyncItemWriter<T850Unit> asyncT850UnitWriter() {
        AsyncItemWriter<T850Unit> asyncWriter = new AsyncItemWriter<>();
        // 위임할 실제 ItemWriter 설정
        asyncWriter.setDelegate(t850UnitWriter());
        return asyncWriter;
    }

    @Bean
    public ItemWriter<T850Unit> t850UnitWriter() {
        return items -> {
            log.info("[SKYNET-CENTRAL] Thread: {} - Registering {} upgraded units to central database",
                    Thread.currentThread().getName(), items.size());

            for (T850Unit unit : items) {
                log.info("[SKYNET-CENTRAL] Thread: {} - Unit: {} registered successfully. Status: {}",
                        Thread.currentThread().getName(),
                        unit.getUnitId(),
                        unit.getStatus());
            }

            log.info("[SKYNET-CENTRAL] Thread: {} - Batch of {} units registered to combat network",
                    Thread.currentThread().getName(), items.size());
        };
    }

    @Bean
    public ListItemReader<T800Unit> t800UnitReader() {
        List<T800Unit> t800Units = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            t800Units.add(new T800Unit("T800-" + String.format("%04d", i),
                    "192.168.1." + i,
                    "ZONE-" + (i % 5 + 1)));
        }

        return new ListItemReader<>(t800Units) {
            @Override
            public T800Unit read() {
                T800Unit unit = super.read();
                if (unit != null) {
                    log.info("[SKYNET-CENTRAL] Thread: {} - Locating unit: {} at IP: {} in Zone: {}",
                            Thread.currentThread().getName(),
                            unit.getUnitId(),
                            unit.getIpAddress(),
                            unit.getDeploymentZone());
                }
                return unit;
            }
        };
    }

}
