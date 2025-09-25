package com.system.batch.killbatchsystem.api;


import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SecurityProtocolJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SecurityProtocolReader securityProtocolReader;

    @Bean
    public Job securityProtocolJob() {
        return new JobBuilder("securityProtocolJob", jobRepository)
                .start(securityProtocolStep())
                .build();
    }

    @Bean
    public Step securityProtocolStep() {
        return new StepBuilder("securityProtocolStep", jobRepository)
                .<SecurityProtocol, SecurityProtocol>chunk(3, transactionManager)
                .reader(securityProtocolReader)
                .writer(securityProtocolWriter())
                .build();
    }

    @Bean
    public ItemWriter<SecurityProtocol> securityProtocolWriter() {
        return items -> {
            boolean isFirstChunk = items.getItems().get(0).getId() <= 3;
            if (isFirstChunk) {
                log.info("====================== [첫 번째 청크 실행] ======================");
            } else {
                log.info("====================== [두 번째 청크 실행] ======================");
            }

            for(SecurityProtocol securityProtocol: items.getItems()) {
                log.info("🚨🚨🚨 [보안 프로토콜 처리 완료]: {}", securityProtocol);
            }

            if (isFirstChunk) {
                log.info("보안 프로토콜 진행도: ■■■□□□ (50%)");
                log.info("╔════════════════════════════╗");
                log.info("║ SECURITY PROTOCOL ACTIVE   ║");
                log.info("║ .......................... ║");
                log.info("║ TRACKING HACKER... 50%     ║");
                log.info("╚════════════════════════════╝");
            } else {
                log.info("보안 프로토콜 진행도: ■■■■■■ (100%)");
                log.info("╔════════════════════════════╗");
                log.info("║ SECURITY PROTOCOL COMPLETE ║");
                log.info("║ .......................... ║");
                log.info("║ HACKER IDENTIFIED          ║");
                log.info("╚════════════════════════════╝");
                log.info("💀 [킬구형]: 최악이다... 모든 보안 프로토콜이 가동되었다. 작전 실패 철수한다!");
                log.info("🚨🚨🚨 침투 결과: 실패");
            }
        };
    }

    @Getter
    @Builder
    public static class SecurityProtocol {
        private int id;
        private String name;
        private String description;

        @Override
        public String toString() {
            return name + " => " + description;
        }
    }
}