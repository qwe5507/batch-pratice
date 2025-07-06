package com.system.batch.killbatchsystem.json.read;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.separator.JsonRecordSeparatorPolicy;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.json.builder.JsonItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

//@Configuration
public class RecordSystemKillJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ObjectMapper objectMapper;  // 새로운 무기 추가

    public RecordSystemKillJobConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ObjectMapper objectMapper
    ) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.objectMapper = objectMapper;
    }

    @Bean
    public Job systemKillJob() {
        return new JobBuilder("recordSystemKillJob", jobRepository)
                .start(systemKillStep())
                .build();
    }

    @Bean
    public Step systemKillStep() {
        return new StepBuilder("recordSystemKillStep", jobRepository)
                .<SystemDeath, SystemDeath>chunk(10, transactionManager)
                .reader(systemKillReader(null))
                .writer(items -> items.forEach(System.out::println))
                .build();
    }

//    @Bean
//    @StepScope
//    public FlatFileItemReader<SystemDeath> systemKillReader(
//            @Value("#{jobParameters['inputFile']}") String inputFile) {
//        return new FlatFileItemReaderBuilder<SystemDeath>()
//                .name("recordSystemKillReader")
//                .resource(new FileSystemResource(inputFile))
//                .lineMapper((line, lineNumber) -> objectMapper.readValue(line, SystemDeath.class))
//                .recordSeparatorPolicy(new JsonRecordSeparatorPolicy())  // JSON 경계 감지기 장착
//                .build();
//    }

    @Bean
    @StepScope
    public JsonItemReader<SystemDeath> systemKillReader(
            @Value("#{jobParameters['inputFile']}") String inputFile) {
        return new JsonItemReaderBuilder<SystemDeath>()
                .name("jsonSystemKillReader")
                .jsonObjectReader(new JacksonJsonObjectReader<>(SystemDeath.class))
                .resource(new FileSystemResource(inputFile))
                .build();
    }

    public record SystemDeath(String command, int cpu, String status) {}

}
