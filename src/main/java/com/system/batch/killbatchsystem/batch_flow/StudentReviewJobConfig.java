package com.system.batch.killbatchsystem.batch_flow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Random;

@Slf4j
@Configuration
public class StudentReviewJobConfig {
    @Bean
    public Job studentReviewJob(JobRepository jobRepository,
                                Step analyzeStudentReviewStep,
                                StudentReviewDecider studentReviewDecider,
                                Step promoteCourseStep,
                                Step normalManagementStep,
                                Step improvementRequiredStep,
                                Step springBatchMasterStep) {
        return new JobBuilder("studentReviewJob", jobRepository)
                .start(analyzeStudentReviewStep)
                .next(studentReviewDecider) // 이 놈을 주목하라 🏴‍☠️
                .on("EXCELLENT_COURSE").to(promoteCourseStep)
                .from(studentReviewDecider).on("AVERAGE_COURSE").to(normalManagementStep)
                .from(studentReviewDecider).on("NEEDS_IMPROVEMENT").to(improvementRequiredStep)
                .from(studentReviewDecider).on("666_SPRING_BATCH").to(springBatchMasterStep)
                .end()
                .build();
    }

    @Component
    public static class StudentReviewDecider implements JobExecutionDecider {

        @Override
        public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
            // StepExecution의 ExecutionContext에서 분석 결과 추출
            ExecutionContext executionContext = stepExecution.getExecutionContext();
            int reviewScore = executionContext.getInt("reviewScore");

            log.info("수강생 리뷰 점수 기반 강의 분류 중", reviewScore);

            // 리뷰 점수에 따른 강의 분류
            if (reviewScore > 10) {
                log.error("스프링 배치 마스터 감지!!!");
                return new FlowExecutionStatus("666_SPRING_BATCH");
            } else if (reviewScore >= 8) {
                log.info("우수 강의 감지! 홍보 대상으로 분류");
                return new FlowExecutionStatus("EXCELLENT_COURSE");
            } else if (reviewScore >= 5) {
                log.info("평균 강의 감지. 일반 관리 대상으로 분류");
                return new FlowExecutionStatus("AVERAGE_COURSE");
            } else {
                log.warn("저평가 강의 감지! 개선 필요 대상으로 분류");
                return new FlowExecutionStatus("NEEDS_IMPROVEMENT");
            }
        }
    }


    @Bean
    public Step analyzeStudentReviewStep(JobRepository jobRepository,
                                         PlatformTransactionManager transactionManager,
                                         Tasklet analyzeStudentReviewTasklet) {
        return new StepBuilder("analyzeStudentReviewStep", jobRepository)
                .tasklet(analyzeStudentReviewTasklet, transactionManager)
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet analyzeStudentReviewTasklet() {
        return (contribution, chunkContext) -> {
            // 수강생 리뷰 점수 분석 (샘플을 위해 랜덤 값 사용)
            Random random = new Random();
            int reviewScore = random.nextInt(12);  // 0-11 사이 랜덤 점수

            log.info("수강생 리뷰 분석 중... 평균 점수: {}/10", reviewScore);

            // StepExecution의 ExecutionContext에 분석 결과 저장
            StepExecution stepExecution = contribution.getStepExecution();
            ExecutionContext executionContext = stepExecution.getExecutionContext();
            executionContext.putInt("reviewScore", reviewScore);

            log.info("수강생 리뷰 분석 완료.");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step promoteCourseStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager) {
        return new StepBuilder("promoteCourseStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("⚠️ 우수 강의 감지... 당신은 김영한?...");
                    log.info("우수 강의 홍보 처리 중...");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step normalManagementStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager) {
        return new StepBuilder("normalManagementStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("일반 관리 대상 강의 처리 중...");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step improvementRequiredStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager) {
        return new StepBuilder("improvementRequiredStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.warn("개선 필요 강의 처리 중...");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step springBatchMasterStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager) {
        return new StepBuilder("springBatchMasterStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.error("████████████████████████████████████");
                    log.error("█     스프링 배치 마스터 강의 감지!      █");
                    log.error("█ 다르다.. 심상치 않은 기운이 느껴진다..💀 █");
                    log.error("████████████████████████████████████");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
