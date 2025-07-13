package com.system.batch.killbatchsystem.mongodb.write;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.MongoCursorItemReader;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoCursorItemReaderBuilder;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

@Slf4j
//@Configuration
@RequiredArgsConstructor
public class CafeSquadTrackingJob {
    private final MongoTemplate mongoTemplate;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job trackCafeSquadJob() {
        return new JobBuilder("trackCafeSquadJob", jobRepository)
                .start(trackCafeSquadStep())
                .build();
    }

    @Bean
    public Step trackCafeSquadStep() {
        return new StepBuilder("trackCafeSquadStep", jobRepository)
                .<SuspiciousDevice, SuspiciousDevice>chunk(10, transactionManager)
                .reader(cafeSquadReader())
                .processor(cafeSquadProcessor())
                .writer(cafeSquadWriter())
                .build();
    }

    @Bean
    public MongoCursorItemReader<SuspiciousDevice> cafeSquadReader() {
        return new MongoCursorItemReaderBuilder<SuspiciousDevice>()
                .name("cafeInfiltrationSquadReader")
                .template(mongoTemplate)
                .collection("pangyo_cafe_devices")
                .jsonQuery("""
                {
                   'location': {
                       $in: [
                           'St*rbucks_pangyo',
                           'Fl*nk_cafe',
                           'Col*ctivo'
                       ]
                   },
                   'timestamp': {
                      $gte: ?0,
                      $lt: ?1
                  }
                }
                """)
                .parameterValues(Arrays.asList(
                        Date.from(LocalDateTime.now().withHour(1).withMinute(0)
                                .atZone(ZoneId.systemDefault()).toInstant()),
                        Date.from(LocalDateTime.now().withHour(17).withMinute(0)
                                .atZone(ZoneId.systemDefault()).toInstant())
                ))
                .sorts(Map.of("timestamp", Sort.Direction.DESC))
                .targetType(SuspiciousDevice.class)
                .batchSize(10)
                .build();
    }

    @Bean
    public ItemProcessor<SuspiciousDevice, SuspiciousDevice> cafeSquadProcessor() {
        return device -> {
            if (isSuspiciousMacPattern(device.getMacAddress())) {
                // MAC 주소를 알 수 없는 값으로 변조
                device.setMacAddress("HA:CK:ED:13:37:666");
                return device;
            }
            return null;
        };
    }

    private boolean isSuspiciousMacPattern(String macAddress) {
        // 수상한 MAC 주소 패턴 체크 로직
        // 예: 특정 제조사 MAC 주소 범위, 알려진 해킹 도구의 MAC 패턴 등
        return true; // 임시로 모든 MAC 주소를 수상하다고 판단
    }

    @Bean
    public MongoItemWriter<SuspiciousDevice> cafeSquadWriter() {
        return new MongoItemWriterBuilder<SuspiciousDevice>()
                .template(mongoTemplate)
                .collection("pangyo_cafe_devices")
                .mode(MongoItemWriter.Mode.UPSERT)  // 기존 문서 수정
                .build();
    }
}
