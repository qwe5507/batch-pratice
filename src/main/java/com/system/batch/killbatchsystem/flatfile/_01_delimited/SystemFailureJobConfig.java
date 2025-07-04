package com.system.batch.killbatchsystem.flatfile._01_delimited;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
//@Configuration
public class SystemFailureJobConfig {
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Bean
    public Job systemFailureJob() {
        return new JobBuilder("systemFailureJob", jobRepository)
                .start(systemFailureStep())
                .build();
    }

    @Bean
    public Step systemFailureStep() {
        return new StepBuilder("systemFailureStep", jobRepository)
                .<SystemFailure, SystemFailure>chunk(10, transactionManager)
                .reader(systemFailureItemReader(null))
                .writer(systemFailureStdoutItemWriter())
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<SystemFailure> systemFailureItemReader(
            @Value("#{jobParameters['inputFile']}") String inputFile) {
        return new FlatFileItemReaderBuilder<SystemFailure>()
                .name("systemFailureItemReader")
                .resource(new FileSystemResource(inputFile))
                .delimited()
                .delimiter(",")
                .names("errorId",
                        "errorDateTime",
                        "severity",
                        "processId",
                        "errorMessage")
                .targetType(SystemFailure.class)
                .linesToSkip(1)
                .build();
    }

    @Bean
    public SystemFailureStdoutItemWriter systemFailureStdoutItemWriter() {
        return new SystemFailureStdoutItemWriter();
    }

    public static class SystemFailureStdoutItemWriter implements ItemWriter<SystemFailure> {
        @Override
        public void write(Chunk<? extends SystemFailure> chunk) throws Exception {
            for (SystemFailure failure : chunk) {
                log.info("Processing system failure: {}", failure);
            }
        }
    }

    public static class SystemFailure {
        private String errorId;
        private String errorDateTime;
        private String severity;
        private Integer processId;
        private String errorMessage;
        // setter, toString() rm -rf


        @Override
        public String toString() {
            return "SystemFailure{" +
                    "errorId='" + errorId + '\'' +
                    ", errorDateTime='" + errorDateTime + '\'' +
                    ", severity='" + severity + '\'' +
                    ", processId=" + processId +
                    ", errorMessage='" + errorMessage + '\'' +
                    '}';
        }

        public void setErrorId(final String errorId) {
            this.errorId = errorId;
        }

        public void setErrorDateTime(final String errorDateTime) {
            this.errorDateTime = errorDateTime;
        }

        public void setSeverity(final String severity) {
            this.severity = severity;
        }

        public void setProcessId(final Integer processId) {
            this.processId = processId;
        }

        public void setErrorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
