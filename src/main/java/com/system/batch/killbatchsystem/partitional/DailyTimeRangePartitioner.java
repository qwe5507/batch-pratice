package com.system.batch.killbatchsystem.partitional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/*
 * 날짜 기반으로 24시간 데이터를 시간대별로 파티셔닝하는 Partitioner (일일 배치용)
 * JobParameter로 받은 targetDate의 00:00:00 부터 다음 날 00:00:00 까지의 범위를
 * gridSize에 따라 분할하여 각 파티션의 시작/종료 Instant를 ExecutionContext에 저장한다.
 */
@Slf4j
@JobScope
@Component
public class DailyTimeRangePartitioner implements Partitioner {
    private final LocalDate targetDate;

    public DailyTimeRangePartitioner(
            @Value("#{jobParameters['targetDate']}") LocalDate targetDate) {
        log.info("Initializing DailyTimeRangePartitioner for targetDate: {}", targetDate);
        this.targetDate = targetDate;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        /*
         *  💀 gridSize(파티션 개수)가 24(Hours)의 약수인지 확인
         *  구현 나름이지만, gridSize가
         *  전체 데이터 크기의 약수가 아니면 던져버리는게 맘 편하다. 💀
         */
        if (24 % gridSize != 0) {
            /*
             * gridSize가 전체 데이터 크기의 약수가 되면
             * 각 파티션이 정확히 같은 시간 범위를 갖게 되어
             * 시스템 부하가 균등하게 분산되고, 행동을 예측하기 쉬워진다.
             * 또한 파티션 크기 분배 로직이 단순해진다. 💀
             */
            throw new IllegalArgumentException("gridSize must be a divisor of 24 (1, 2, 3, 4, 6, 8, 12, or 24)");
        }

        Map<String, ExecutionContext> partitions = new HashMap<>(gridSize);

        // 💀 targetDate의 시작(00:00:00)과 종료(다음 날 00:00:00) 시점을 계산 💀
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();


        log.info("Creating {} partitions for time range: {} to {}",
                gridSize, startOfDay, endOfDay);

        // 💀 각 시간대별로 파티션 생성 💀
        int partitionHours = 24 / gridSize;

        // 💀 각 파티션의 시작/종료 시간 계산 및 ExecutionContext 생성 💀
        for (int i = 0; i < gridSize; i++) {
            LocalDateTime partitionStartDateTime = startOfDay.plusHours(i * partitionHours);
            LocalDateTime partitionEndDateTime = partitionStartDateTime.plusHours(partitionHours);

            /*
             * 💀 gridSize가 24시간의 약수가 아닌 경우에는
             * 마지막 파티션이 다른 파티션보다 더 작거나 클 수 있다.
             * 이 때 endTime 설정이 필수적이다.
             * 이렇게 하면 모든 시간대의 데이터가 파티션에 포함되도록 보장할 수 있다. 💀
             */
            // if (i == gridSize - 1) {
            //     partitionEndTime = endOfDay;
            // }

            // 💀 파티션별 ExecutionContext에 시간 범위 정보 저장 💀
            ExecutionContext context = new ExecutionContext();
            context.put("startDateTime", partitionStartDateTime);
            context.put("endDateTime", partitionEndDateTime);

            log.info("Partition {}: {} to {}", i, partitionStartDateTime, partitionEndDateTime);

            partitions.put(String.valueOf(i), context);
        }

        return partitions;
    }
}
