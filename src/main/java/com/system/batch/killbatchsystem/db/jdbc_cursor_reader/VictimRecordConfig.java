package com.system.batch.killbatchsystem.db.jdbc_cursor_reader;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class VictimRecordConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    @Bean
    public Job processVictimJob() {
        return new JobBuilder("victimRecordJob", jobRepository)
                .start(processVictimStep())
                .build();
    }

    @Bean
    public Step processVictimStep() {
        return new StepBuilder("victimRecordStep", jobRepository)
                .<Victim, Victim>chunk(5, transactionManager)
                .reader(terminatedVictimReader())
                .writer(victimWriter())
                .build();
    }

    @Bean
    public JdbcCursorItemReader<Victim> terminatedVictimReader() {
        return new JdbcCursorItemReaderBuilder<Victim>()
                .name("terminatedVictimReader")
                .dataSource(dataSource)
                .sql("SELECT * FROM victims WHERE status = ? AND terminated_at <= ?")
                .queryArguments(List.of("TERMINATED", LocalDateTime.now()))
//                .beanRowMapper(Victim.class)  // 객체 변환 (BeanPropertyRowMapper)
//                .rowMapper((rs, rowNum) -> { // 커스텀 rowMapper 람다 정의 가능
//                    Victim victim = new Victim();
//                    victim.setId(rs.getLong("id"));
//                    victim.setName(rs.getString("name"));
//                    victim.setProcessId(rs.getString("process_id"));
//                    victim.setTerminatedAt(rs.getTimestamp("terminated_at").toLocalDateTime());
//                    victim.setStatus(rs.getString("status"));
//                    return victim;
//                })
                .rowMapper(new DataClassRowMapper<>(Victim.class)) // 레코드 및 코들린 객체 매핑
                .build();
    }
    // batch 5.2 버전부터는 JdbcCursorItemReaderBuilder에
    // dataRowMapper(레코드나 코트린객체) 추가하면 자동으로 매핑된다고함

    @Bean
    public ItemWriter<Victim> victimWriter() {
        return items -> {
            for (Victim victim : items) {
                log.info("{}", victim);
            }
        };
    }

//    @NoArgsConstructor
//    @Data
//    public static class Victim {
//        private Long id;
//        private String name;
//        private String processId;
//        private LocalDateTime terminatedAt;
//        private String status;
//    }
    public record Victim(Long id, String name, String processId,
                         LocalDateTime terminatedAt, String status) {}
}
