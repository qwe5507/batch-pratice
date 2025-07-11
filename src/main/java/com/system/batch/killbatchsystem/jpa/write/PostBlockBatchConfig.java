package com.system.batch.killbatchsystem.jpa.write;

import jakarta.persistence.EntityManagerFactory;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.orm.JpaNamedQueryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class PostBlockBatchConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final PostBlockProcessor postBlockProcessor;

    @Bean
    public Job postBlockBatchJob() {
        return new JobBuilder("postBlockBatchJob", jobRepository)
                .start(postBlockStep())
                .build();
    }

    @Bean
    public Step postBlockStep() {
        return new StepBuilder("postBlockStep", jobRepository)
                .<Post, Post>chunk(5, transactionManager)
                .reader(postBlockReader())
                .processor(postBlockProcessor)
                .writer(postBlockWriter())
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    public JpaPagingItemReader<Post> postBlockReader() {
        return new JpaPagingItemReaderBuilder<Post>()
                .name("postBlockReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT p FROM Post p WHERE p.blockedAt IS NULL ORDER BY p.id ASC") // JOIN FETCH 제거
                .pageSize(5) // 페이지 사이즈 지정
                .transacted(false) // false 설정
                .build();
    }

    private JpaNamedQueryProvider<Post> createQueryProvider() {
        JpaNamedQueryProvider<Post> queryProvider = new JpaNamedQueryProvider<>();
        queryProvider.setEntityClass(Post.class);
        queryProvider.setNamedQuery("Post.findTodayUnblockedWithReports");
        return queryProvider;
    }

    @Bean
    public JpaItemWriter<Post> postBlockWriter() {
        return new JpaItemWriterBuilder<Post>()
                .entityManagerFactory(entityManagerFactory)
//                .usePersist(true) // insert 는 persist 사용(true), update 는 merge 사용(false)
                .usePersist(false) // merge 사용
                .build();
    }

    @Component
    @RequiredArgsConstructor
//    public static class PostBlockProcessor implements ItemProcessor<Post, BlockedPost> {
    public static class PostBlockProcessor implements ItemProcessor<Post, Post> {

//        @Override
//        public BlockedPost process(Post post) {
//            // 각 신고의 신뢰도를 기반으로 차단 점수 계산
//            double blockScore = calculateBlockScore(post.getReports());
//
//            // 차단 점수가 기준치를 넘으면 처형 결정
//            if (blockScore >= 7.0) {
//                return BlockedPost.builder()
//                        .postId(post.getId())
//                        .writer(post.getWriter())
//                        .title(post.getTitle())
//                        .reportCount(post.getReports().size())
//                        .blockScore(blockScore)
//                        .blockedAt(LocalDateTime.now())
//                        .build();
//            }
//
//            return null;  // 무죄 방면
//        }
        @Override
        public Post process(Post post) {
            // 각 신고의 신뢰도를 기반으로 차단 점수 계산
            double blockScore = calculateBlockScore(post.getReports());

            // 차단 점수가 기준치를 넘으면 처형 결정
            if (blockScore >= 7.0) {
                post.setBlockedAt(LocalDateTime.now());
                return post;
            }

            return null;  // 무죄 방면
        }

        private double calculateBlockScore(List<Report> reports) {
            // 각 신고들의 정보를 시그니처에 포함시켜 마치 사용하는 것처럼 보이지만...
            for (Report report : reports) {
                analyzeReportType(report.getReportType());            // 신고 유형 분석
                checkReporterTrust(report.getReporterLevel());        // 신고자 신뢰도 확인
                validateEvidence(report.getEvidenceData());           // 증거 데이터 검증
                calculateTimeValidity(report.getReportedAt());        // 시간 가중치 계산
            }

            // 실제로는 그냥 랜덤 값을 반환
            return Math.random() * 10;  // 0~10 사이의 랜덤 값
        }

        // 아래는 실제로는 아무것도 하지 않는 메서드들
        private void analyzeReportType(String reportType) {
            // 신고 유형 분석하는 척
        }

        private void checkReporterTrust(int reporterLevel) {
            // 신고자 신뢰도 확인하는 척
        }

        private void validateEvidence(String evidenceData) {
            // 증거 검증하는 척
        }

        private void calculateTimeValidity(LocalDateTime reportedAt) {
            // 시간 가중치 계산하는 척
        }
    }
}
