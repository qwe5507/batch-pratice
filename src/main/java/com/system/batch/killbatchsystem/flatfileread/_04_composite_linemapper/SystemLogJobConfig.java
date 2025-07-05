package com.system.batch.killbatchsystem.flatfileread._04_composite_linemapper;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.mapping.PatternMatchingCompositeLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.validation.BindException;

import java.util.HashMap;
import java.util.Map;

// 패턴이 뒤죽박죽일 때 패턴 매칭을 통해 서로 다른 LineTokenizer와 FieldSetMapper를 적용할 수 있다.
@Slf4j
@Configuration
public class SystemLogJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    public SystemLogJobConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    @Bean
    public Job systemLogJob() {
        return new JobBuilder("systemLogJob", jobRepository)
                .start(systemLogStep())
                .build();
    }

    @Bean
    public Step systemLogStep() {
        return new StepBuilder("systemLogStep", jobRepository)
                .<SystemLog, SystemLog>chunk(10, transactionManager)
                .reader(systemLogReader(null))
                .writer(systemLogWriter())
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<SystemLog> systemLogReader(
            @Value("#{jobParameters['inputFile']}") String inputFile) {
        return new FlatFileItemReaderBuilder<SystemLog>()
                .name("systemLogReader")
                .resource(new FileSystemResource(inputFile))
                .lineMapper(systemLogLineMapper())
                .build();
    }

    @Bean
    public PatternMatchingCompositeLineMapper<SystemLog> systemLogLineMapper() {
        PatternMatchingCompositeLineMapper<SystemLog> lineMapper = new PatternMatchingCompositeLineMapper<>();

        Map<String, LineTokenizer> tokenizers = new HashMap<>();
        tokenizers.put("ERROR*", errorLineTokenizer());
        tokenizers.put("ABORT*", abortLineTokenizer());
        tokenizers.put("COLLECT*", collectLineTokenizer());
        lineMapper.setTokenizers(tokenizers);

        Map<String, FieldSetMapper<SystemLog>> mappers = new HashMap<>();
        mappers.put("ERROR*", new ErrorFieldSetMapper());
        mappers.put("ABORT*", new AbortFieldSetMapper());
        mappers.put("COLLECT*", new CollectFieldSetMapper());
        lineMapper.setFieldSetMappers(mappers);

        return lineMapper;
    }

    @Bean
    public DelimitedLineTokenizer errorLineTokenizer() {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(",");
        tokenizer.setNames("type", "application", "errorType", "timestamp", "message", "resourceUsage", "logPath");
        return tokenizer;
    }

    @Bean
    public DelimitedLineTokenizer abortLineTokenizer() {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(",");
        tokenizer.setNames("type", "application", "errorType", "timestamp", "message", "exitCode", "processPath", "status");
        return tokenizer;
    }

    @Bean
    public DelimitedLineTokenizer collectLineTokenizer() {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(",");
        tokenizer.setNames("type", "dumpType", "processId", "timestamp", "dumpPath");
        return tokenizer;
    }

    @Bean
    public ItemWriter<SystemLog> systemLogWriter() {
        return items -> {
            for (SystemLog item : items) {
                log.info(""+item);
            }
        };
    }

    @Data
    public static class SystemLog {
        private String type;
        private String timestamp;

        public void setType(final String type) {
            this.type = type;
        }

        public void setTimestamp(final String timestamp) {
            this.timestamp = timestamp;
        }
    }

    @Data
    @ToString(callSuper = true)
    public static class ErrorLog extends SystemLog {
        private String application;
        private String errorType;
        private String message;
        private String resourceUsage;
        private String logPath;

        public void setApplication(final String application) {
            this.application = application;
        }

        public void setErrorType(final String errorType) {
            this.errorType = errorType;
        }

        public void setMessage(final String message) {
            this.message = message;
        }

        public void setResourceUsage(final String resourceUsage) {
            this.resourceUsage = resourceUsage;
        }

        public void setLogPath(final String logPath) {
            this.logPath = logPath;
        }
    }

    @Data
    @ToString(callSuper = true)
    public static class AbortLog extends SystemLog {
        private String application;
        private String errorType;
        private String message;
        private String exitCode;
        private String processPath;
        private String status;

        public void setApplication(final String application) {
            this.application = application;
        }

        public void setErrorType(final String errorType) {
            this.errorType = errorType;
        }

        public void setMessage(final String message) {
            this.message = message;
        }

        public void setExitCode(final String exitCode) {
            this.exitCode = exitCode;
        }

        public void setProcessPath(final String processPath) {
            this.processPath = processPath;
        }

        public void setStatus(final String status) {
            this.status = status;
        }
    }

    @Data
    @ToString(callSuper = true)
    public static class CollectLog extends SystemLog {
        private String dumpType;
        private String processId;
        private String dumpPath;

        public void setDumpType(final String dumpType) {
            this.dumpType = dumpType;
        }

        public void setProcessId(final String processId) {
            this.processId = processId;
        }

        public void setDumpPath(final String dumpPath) {
            this.dumpPath = dumpPath;
        }
    }

    public static class ErrorFieldSetMapper implements FieldSetMapper<SystemLog> {
        @Override
        public SystemLog mapFieldSet(FieldSet fs) throws BindException {
            ErrorLog errorLog = new ErrorLog();
            errorLog.setType(fs.readString("type"));
            errorLog.setApplication(fs.readString("application"));
            errorLog.setErrorType(fs.readString("errorType"));
            errorLog.setTimestamp(fs.readString("timestamp"));
            errorLog.setMessage(fs.readString("message"));
            errorLog.setResourceUsage(fs.readString("resourceUsage"));
            errorLog.setLogPath(fs.readString("logPath"));
            return errorLog;
        }
    }

    public static class AbortFieldSetMapper implements FieldSetMapper<SystemLog> {
        @Override
        public SystemLog mapFieldSet(FieldSet fs) throws BindException {
            AbortLog abortLog = new AbortLog();
            abortLog.setType(fs.readString("type"));
            abortLog.setApplication(fs.readString("application"));
            abortLog.setErrorType(fs.readString("errorType"));
            abortLog.setTimestamp(fs.readString("timestamp"));
            abortLog.setMessage(fs.readString("message"));
            abortLog.setExitCode(fs.readString("exitCode"));
            abortLog.setProcessPath(fs.readString("processPath"));
            abortLog.setStatus(fs.readString("status"));
            return abortLog;
        }
    }

    public static class CollectFieldSetMapper implements FieldSetMapper<SystemLog> {
        @Override
        public SystemLog mapFieldSet(FieldSet fs) throws BindException {
            CollectLog collectLog = new CollectLog();
            collectLog.setType(fs.readString("type"));
            collectLog.setDumpType(fs.readString("dumpType"));
            collectLog.setProcessId(fs.readString("processId"));
            collectLog.setTimestamp(fs.readString("timestamp"));
            collectLog.setDumpPath(fs.readString("dumpPath"));
            return collectLog;
        }
    }
}
