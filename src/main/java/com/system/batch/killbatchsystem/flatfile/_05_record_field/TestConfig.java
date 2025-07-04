package com.system.batch.killbatchsystem.flatfile._05_record_field;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

// RecordFieldSetMapper는 record의 canonical constructor(모든 필드를 매개변수로 받는 생성자)를 사용해 데이터를 매핑한다.
// RecordFieldSetMapper는 테스트 안해봄.
@Slf4j
@Configuration
public class TestConfig {
    @Bean
    @StepScope
    public FlatFileItemReader<SystemDeath> systemKillReader(
            @Value("#{jobParameters['inputFile']}") String inputFile) {
        return new FlatFileItemReaderBuilder<SystemDeath>()
                .name("systemKillReader")
                .resource(new FileSystemResource(inputFile))
                .delimited()
                .names("command", "cpu", "status")
                .targetType(SystemDeath.class)
                .linesToSkip(1)
                .build();
    }
}
