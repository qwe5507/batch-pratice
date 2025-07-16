package com.system.batch.killbatchsystem.retry;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class TerminationRetryConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job terminationRetryJob() {
        return new JobBuilder("terminationRetryJob", jobRepository)
                .start(terminationRetryStep())
                .build();
    }

    @Bean
    public Step terminationRetryStep() {
        return new StepBuilder("terminationRetryStep", jobRepository)
                .<Scream, Scream>chunk(3, transactionManager)
                .reader(terminationRetryReader())
                .processor(terminationRetryProcessor())
                .writer(terminationRetryWriter())
                .faultTolerant() // 내결함성 기능 ON
                .retry(TerminationFailedException.class) // 재시도 대상 예외 추가
                .retryLimit(3)
                .listener(retryListener())
//                .processorNonTransactional() // ItemProcessor 비트랜잭션 처리
                // BackOffPolicy 설정
//                .backOffPolicy(new FixedBackOffPolicy() {{
//                    setBackOffPeriod(1000); // 1초
//                }})
                // 또는
                .backOffPolicy(new ExponentialBackOffPolicy() {{
                    setInitialInterval(1000L);  // 초기 대기 시간
                    setMultiplier(2.0);        // 대기 시간 증가 배수
                    setMaxInterval(10000L);     // 최대 대기 시간
                }})
                .build();
    }

    @Bean
    public ListItemReader<Scream> terminationRetryReader() {
        return new ListItemReader<>(List.of(
                Scream.builder()
                        .id(1)
                        .scream("멈춰")
                        .processMsg("멈추라고 했는데 안 들음.")
                        .build(),
                Scream.builder()
                        .id(2)
                        .scream("제발")
                        .processMsg("애원 소리 귀찮네.")
                        .build(),
                Scream.builder()
                        .id(3)
                        .scream("살려줘")
                        .processMsg("구조 요청 무시.")
                        .build(),
                Scream.builder()
                        .id(4)
                        .scream("으악")
                        .processMsg("디스크 터지며 울부짖음.")
                        .build(),
                Scream.builder()
                        .id(5)
                        .scream("끄아악")
                        .processMsg("메모리 붕괴 비명.")
                        .build(),
                Scream.builder()
                        .id(6)
                        .scream("System.exit(-666)")
                        .processMsg("초살 프로토콜 발동.")
                        .build()
        )) {
            @Override
            public Scream read() {
                Scream scream = super.read();
                if(scream == null) {
                    return null;
                }
                System.out.println("🔥🔥🔥 [ItemReader]: 처형 대상 = " + scream);
                return scream;
            }
        };
    }

//    @Bean
//    public ItemProcessor<Scream, Scream> terminationRetryProcessor() {
//        return new ItemProcessor<>() {
//            private static final int MAX_PATIENCE = 3;
//            private int mercy = 0;  // 자비 카운트
//
//            @Override
//            public Scream process(Scream scream) throws Exception {
//                System.out.print("🔥🔥🔥 [ItemProcessor]: 처형 대상 = " + scream);
//
//                if (scream.getId() == 3 && mercy < MAX_PATIENCE) {
//                    mercy ++;
//                    System.out.println(" -> ❌ 처형 실패.");
//                    throw new TerminationFailedException("처형 거부자 = " + scream);
//                } else {
//                    System.out.println(" -> ✅ 처형 완료(" + scream.getProcessMsg() + ")");
//                }
//
//                return scream;
//            }
//        };
//    }

    @Bean
    public ItemProcessor<Scream, Scream> terminationRetryProcessor() {
        return scream -> {
            System.out.print("🔥🔥🔥 [ItemProcessor]: 처형 대상 = " + scream + "\n");
            return scream;
        };
    }

//    @Bean
//    public ItemWriter<Scream> terminationRetryWriter() {
//        return items -> {
//            System.out.println("🔥🔥🔥 [ItemWriter]: 처형 기록 시작. 기록 대상 = " + items.getItems());
//
//            for (Scream scream : items) {
//                System.out.println("🔥🔥🔥 [ItemWriter]: 기록 완료. 처형된 아이템 = " + scream);
//            }
//        };
//    }

    @Bean
    public ItemWriter<Scream> terminationRetryWriter() {
        return new ItemWriter<>() {
            private static final int MAX_PATIENCE = 3;
            private int mercy = 0;

            @Override
            public void write(Chunk<? extends Scream> screams) {
                System.out.println("🔥🔥🔥 [ItemWriter]: 기록 시작. 처형된 아이템들 = " + screams);
                mercy ++;

                for (Scream scream : screams) {
                    if (scream.getId() == mercy && mercy <= MAX_PATIENCE) {
                        System.out.println("🔥🔥🔥 [ItemWriter]: ❌ 기록 실패. 저항하는 아이템 발견 = " + scream);
                        throw new TerminationFailedException("기록 거부자 = " + scream);
                    }
                    System.out.println("🔥🔥🔥 [ItemWriter]: ✅ 기록 완료. 처형된 아이템 = " + scream);
                }

            }
        };
    }

    @Bean
    public RetryListener retryListener() {
        return new RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                System.out.println("💀💀💀 킬구형: 이것 봐라? 안 죽네? " + throwable + " (현재 총 시도 횟수=" + context.getRetryCount() + "). 다시 처형한다.\n");
            }
        };
    }

    public static class TerminationFailedException extends RuntimeException {
        public TerminationFailedException(String message) {
            super(message);
        }
    }

    @Getter
    @Builder
    public static class Scream {
        private int id;
        private String scream;
        private String processMsg;

        @Override
        public String toString() {
            return id + "_" + scream;
        }
    }
}
