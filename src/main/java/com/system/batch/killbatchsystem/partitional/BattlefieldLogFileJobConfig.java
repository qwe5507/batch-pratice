package com.system.batch.killbatchsystem.partitional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.SimpleSystemProcessExitCodeMapper;
import org.springframework.batch.core.step.tasklet.SystemCommandTasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BattlefieldLogFileJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job battlefieldLogFileJob(
            Step logFileManagerStep,
            Step mergeOutputFilesStep
    ) {
        return new JobBuilder("battlefieldLogFileJob", jobRepository)
                .start(logFileManagerStep)
                .next(mergeOutputFilesStep)
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    public Step logFileManagerStep(
            Step logFileWorkerStep,
            Partitioner partitioner
    ) {
        return new StepBuilder("logFileManagerStep", jobRepository)
                .partitioner("logFileWorkerStep", partitioner)
                .step(logFileWorkerStep)
                // 💀 .gridSize() 설정 불필요! DEFAULT_GRID_SIZE = 6 💀
                .taskExecutor(logFilePartitionTaskExecutor())
                .build();
    }

    @Bean
    public Step logFileWorkerStep(
            FlatFileItemReader<BattlefieldLog> battlefieldLogReader,
            ItemProcessor<BattlefieldLog, BattlefieldLog> battlefieldLogProcessor,
            FlatFileItemWriter<BattlefieldLog> battlefieldLogFileWriter
    ) {
        return new StepBuilder("logFileWorkerStep", jobRepository)
                .<BattlefieldLog, BattlefieldLog>chunk(100, transactionManager)
                .reader(battlefieldLogReader)
                .processor(battlefieldLogProcessor)
                .writer(battlefieldLogFileWriter)
                .build();
    }

    @Bean
    public Step mergeOutputFilesStep(SystemCommandTasklet mergeFilesTasklet) {
        return new StepBuilder("mergeOutputFilesStep", jobRepository)
                .tasklet(mergeFilesTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Partitioner partitioner(@Value("#{jobParameters['path']}") String path) {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

        try {
            Resource[] resources = resourcePatternResolver.getResources("file://" + path + "/*.csv");
            log.info("Found {} resources to process", resources.length);
            partitioner.setResources(resources);
        } catch (IOException e) {
            throw new IllegalArgumentException("치명적 오류: 출력 파일 경로 생성 실패! 입력 URL: " + path, e);
        }

        return partitioner;
    }

    @Bean
    @StepScope
    public FlatFileItemReader<BattlefieldLog> battlefieldLogReader(@Value("#{stepExecutionContext['fileName']}") String fileName) {
        log.info("Creating reader for file: {}", fileName);
        ResourcePatternResolver resourceLoader = new PathMatchingResourcePatternResolver();

        return new FlatFileItemReaderBuilder<BattlefieldLog>()
                .name("battlefieldLogReader")
                .resource(resourceLoader.getResource(fileName))
                .linesToSkip(1)
                .delimited()
                .names("id", "timestamp", "region", "source", "level", "category", "message")
                .targetType(BattlefieldLog.class)
                .customEditors(Map.of(LocalDateTime.class, dateTimeEditor()))
                .build();
    }

    private PropertyEditor dateTimeEditor() {
        return new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(LocalDateTime.parse(text));
            }
        };
    }

    @Bean
    @StepScope
    public ItemProcessor<BattlefieldLog, BattlefieldLog> battlefieldLogProcessor() {
        return battlefieldLog -> {
            log.info("Thread: {} - Processing log: {}",
                    Thread.currentThread().getName(),
                    battlefieldLog);
            return battlefieldLog;
        };
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<BattlefieldLog> battlefieldLogFileWriter(
            @Value("#{stepExecutionContext['fileName']}") String fileName) {
        String outputFileName;
        try {
            String inputFilePath = new URL(fileName).getPath();
            outputFileName = inputFilePath + ".out";
            log.info("Thread: {} - Configuring writer for output file: {}",
                    Thread.currentThread().getName(), outputFileName);
        } catch (MalformedURLException e) {
            log.error("잘못된 입력 파일 URL 형식: file://{}", fileName, e);
            throw new IllegalArgumentException("출력 파일 경로 생성 실패: " + fileName, e);
        }

        return new FlatFileItemWriterBuilder<BattlefieldLog>()
                .name("battlefieldLogFileWriter")
                .resource(new FileSystemResource(outputFileName))
                .encoding("UTF-8")
                .delimited()
                .names("id", "timestamp", "region", "source", "level", "category", "message")
                .build();
    }

    // 로그만 출력하는 단순 Writer (실제 작전에서는 사용되지 않음)
    //@Bean
    //@StepScope
    public ItemWriter<BattlefieldLog> battlefieldLogWriter() {
        return items -> {
            for (BattlefieldLog logs : items) {
                log.info("Thread: {} - Writing log: {} - {} - {}",
                        Thread.currentThread().getName(),
                        logs.getSource(),
                        logs.getLevel(),
                        logs.getMessage());
            }
        };
    }

    @Bean
    public TaskExecutor logFilePartitionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 파티션 개수(파일 개수)만큼 스레드를 생성하는 것이 이상적이지만,
        // 실제 환경에서는 가용 리소스를 고려하여 적절한 값으로 설정
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setThreadNamePrefix("FilePartition-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }

    @Bean
    @StepScope
    public SystemCommandTasklet mergeFilesTasklet(@Value("#{jobParameters['path']}") String path) {
        // KILL-9: 모든 파티션 처리가 끝나면 이놈이 호출된다! 'cat' 명령으로 흩어진 '.out' 파일들을 하나로 합친다!
        SystemCommandTasklet tasklet = new SystemCommandTasklet();

        String command = String.format("cat %s/*.out > %s/%s", path, path, "merged_battlefield_logs.log");

        log.info("Executing command: {}", command);

        tasklet.setCommand("/bin/sh", "-c", command);
        tasklet.setTimeout(60000L);
        tasklet.setWorkingDirectory(path);
        tasklet.setSystemProcessExitCodeMapper(new SimpleSystemProcessExitCodeMapper());
        return tasklet;
    }
}
