package com.system.batch.killbatchsystem.jpa.cursor;

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
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.orm.JpaNamedQueryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

//@Configuration
@RequiredArgsConstructor
public class PostBlockBatchConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final PostBlockProcessor postBlockProcessor;

//    @Bean
    public Job postBlockBatchJob() {
        return new JobBuilder("postBlockBatchJob", jobRepository)
                .start(postBlockStep())
                .build();
    }

//    @Bean
    public Step postBlockStep() {
        return new StepBuilder("postBlockStep", jobRepository)
                .<Post, BlockedPost>chunk(5, transactionManager)
                .reader(postBlockReader())
                .processor(postBlockProcessor)
                .writer(postBlockWriter())
                .allowStartIfComplete(true)
                .build();
    }

//    @Bean
    public JpaCursorItemReader<Post> postBlockReader() {
        return new JpaCursorItemReaderBuilder<Post>()
                .name("postBlockReader")
                .entityManagerFactory(entityManagerFactory)
                .queryProvider(createQueryProvider()) // queryProvider 설정 시, queryString() 무시
                .parameterValues(Map.of(
                        "today", LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
                ))
//                .hintValues(Map.of("org.hibernate.fetchSize", 100)) // batch 5.2 버전부터는 hintValues() 사용
//                .queryString("SELECT p FROM Post p JOIN FETCH p.reports r WHERE  p.blockedAt IS NULL")
                .build();
    }

    private JpaNamedQueryProvider<Post> createQueryProvider() {
        JpaNamedQueryProvider<Post> queryProvider = new JpaNamedQueryProvider<>();
        queryProvider.setEntityClass(Post.class);
        queryProvider.setNamedQuery("Post.findTodayUnblockedWithReports");
        return queryProvider;
    }

//    @Bean
    public ItemWriter<BlockedPost> postBlockWriter() {
        return items -> items.forEach(System.out::println);
    }

    /**
     * 차단된 게시글 - 처형 결과 보고서
     */
    @Getter
    @Builder
    @ToString
    public static class BlockedPost {
        private Long postId;
        private String writer;
        private String title;
        private int reportCount;
        private double blockScore;
        private LocalDateTime blockedAt;
    }

//    @Component
    @RequiredArgsConstructor
    public static class PostBlockProcessor implements ItemProcessor<Post, BlockedPost> {

        @Override
        public BlockedPost process(Post post) {
            // 각 신고의 신뢰도를 기반으로 차단 점수 계산
            double blockScore = calculateBlockScore(post.getReports());

            // 차단 점수가 기준치를 넘으면 처형 결정
            if (blockScore >= 7.0) {
                return BlockedPost.builder()
                        .postId(post.getId())
                        .writer(post.getWriter())
                        .title(post.getTitle())
                        .reportCount(post.getReports().size())
                        .blockScore(blockScore)
                        .blockedAt(LocalDateTime.now())
                        .build();
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
