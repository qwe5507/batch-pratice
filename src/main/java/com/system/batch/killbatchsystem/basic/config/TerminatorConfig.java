package com.system.batch.killbatchsystem.basic.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.converter.JsonJobParametersConverter;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
//@Configuration
public class TerminatorConfig {
    public enum QuestDifficulty { EASY, NORMAL, HARD, EXTREME }

    @Bean
    public JobParametersConverter jobParametersConverter() {
        return new JsonJobParametersConverter();
    }

    @Bean
    public Job terminatorJob(JobRepository jobRepository, Step terminationStep) {
        return new JobBuilder("terminatorJob", jobRepository)
                .start(terminationStep)
                .build();
    }

    @Bean
    public Step terminationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, Tasklet terminatorTasklet) {
        return new StepBuilder("terminationStep", jobRepository)
                .tasklet(terminatorTasklet, transactionManager)
                .build();
    }

//    @Bean
//    @StepScope
//    public Tasklet terminatorTasklet(
//            @Value("#{jobParameters['executionDate']}") LocalDate executionDate,
//            @Value("#{jobParameters['startTime']}") LocalDateTime startTime
//    ) {
//        return (contribution, chunkContext) -> {
//            log.info("시스템 처형 정보:");
//            log.info("처형 예정일: {}", executionDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
//            log.info("작전 개시 시각: {}", startTime.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분 ss초")));
//            log.info("⚡ {}에 예정된 시스템 정리 작전을 개시합니다.", executionDate);
//            log.info("💀 작전 시작 시각: {}", startTime);
//
//            // 작전 진행 상황 추적
//            LocalDateTime currentTime = startTime;
//            for (int i = 1; i <= 3; i++) {
//                currentTime = currentTime.plusHours(1);
//                log.info("☠️ 시스템 정리 {}시간 경과... 현재 시각:{}", i, currentTime.format(DateTimeFormatter.ofPattern("HH시 mm분")));
//            }
//
//            log.info("🎯 임무 완료: 모든 대상 시스템이 성공적으로 제거되었습니다.");
//            log.info("⚡ 작전 종료 시각: {}", currentTime.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분 ss초")));
//
//
//            return RepeatStatus.FINISHED;
//        };
//    }

//    @Bean
//    @StepScope
//    public Tasklet terminatorTasklet(
//            @Value("#{jobParameters['questDifficulty']}") QuestDifficulty questDifficulty
//    ) {
//        return (contribution, chunkContext) -> {
//            log.info("⚔️ 시스템 침투 작전 개시!");
//            log.info("임무 난이도: {}", questDifficulty);
//            // 난이도에 따른 보상 계산
//            int baseReward = 100;
//            int rewardMultiplier = switch (questDifficulty) {
//                case EASY -> 1;
//                case NORMAL -> 2;
//                case HARD -> 3;
//                case EXTREME -> 5;
//            };
//            int totalReward = baseReward * rewardMultiplier;
//            log.info("💥 시스템 해킹 진행 중...");
//            log.info("🏆 시스템 장악 완료!");
//            log.info("💰 획득한 시스템 리소스: {} 메가바이트", totalReward);
//            return RepeatStatus.FINISHED;
//        };
//    }

//    @Bean
//    public Tasklet terminatorTasklet(SystemInfiltrationParameters infiltrationParams) {
//        return (contribution, chunkContext) -> {
//            log.info("⚔️ 시스템 침투 작전 초기화!");
//            log.info("임무 코드네임: {}", infiltrationParams.getMissionName());
//            log.info("보안 레벨: {}", infiltrationParams.getSecurityLevel());
//            log.info("작전 지휘관: {}", infiltrationParams.getOperationCommander());
//
//            // 보안 레벨에 따른 침투 난이도 계산
//            int baseInfiltrationTime = 60; // 기본 침투 시간 (분)
//            int infiltrationMultiplier = switch (infiltrationParams.getSecurityLevel()) {
//                case 1 -> 1; // 저보안
//                case 2 -> 2; // 중보안
//                case 3 -> 4; // 고보안
//                case 4 -> 8; // 최고 보안
//                default -> 1;
//            };
//
//            int totalInfiltrationTime = baseInfiltrationTime * infiltrationMultiplier;
//
//            log.info("💥 시스템 해킹 난이도 분석 중...");
//            log.info("🕒 예상 침투 시간: {}분", totalInfiltrationTime);
//            log.info("🏆 시스템 장악 준비 완료!");
//
//            return RepeatStatus.FINISHED;
//        };
//    }

    @Bean
    @StepScope
    public Tasklet terminatorTasklet(
            @Value("#{jobParameters['infiltrationTargets']}") String infiltrationTargets
    ) {
        return (contribution, chunkContext) -> {
            String[] targets = infiltrationTargets.split(",");

            log.info("⚡ 침투 작전 개시");
            log.info("첫 번째 타겟: {} 침투 시작", targets[0]);
            log.info("마지막 타겟: {} 에서 집결", targets[1]);
            log.info("🎯 임무 전달 완료");

            return RepeatStatus.FINISHED;
        };
    }
}