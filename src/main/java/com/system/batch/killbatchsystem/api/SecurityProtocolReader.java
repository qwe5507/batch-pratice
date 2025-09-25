package com.system.batch.killbatchsystem.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.system.batch.killbatchsystem.api.SecurityProtocolJobConfig.*;

@Component
@Slf4j
public class SecurityProtocolReader extends AbstractItemCountingItemStreamItemReader<SecurityProtocol> {

    private List<SecurityProtocol> items = List.of(
            SecurityProtocol.builder().id(1).name("🚨 [침입 감지 시스템]").description("비정상 접근 패턴 감지! 추적 프로토콜 가동...").build(),
            SecurityProtocol.builder().id(2).name("📡 [접근 로그 수집]").description("침입자 활동 로깅 중! IP 주소 및 장치 정보 기록...").build(),
            SecurityProtocol.builder().id(3).name("🔍 [악성 행위 스캔]").description("시스템 변조 시도 감지! 실시간 모니터링 강화...").build(),
            SecurityProtocol.builder().id(4).name("📱 [관리자 알림]").description("보안팀에 비상 경보 전송 중! 대응 인력 배치...").build(),
            SecurityProtocol.builder().id(5).name("🔒 [격리 프로토콜]").description("침입 구간 네트워크 차단! 방화벽 규칙 적용...").build(),
            SecurityProtocol.builder().id(6).name("👮 [포렌식 분석]").description("침입자 행적 증거 수집 중! 법적 조치 준비...").build()
    );

    private int index = 0;

    public SecurityProtocolReader() {
        setName("securityProtocolReader");
    }

    @Override
    protected SecurityProtocol doRead() {
        if (index >= items.size()) {
            return null;
        }

        if (index == 0) {
            log.info("====================== [침입 감지] ======================");
            log.info("🚨🚨🚨 [보안 시스템] 해커 침입 감지! 보안 프로토콜 가동 중...");
            log.info("╔═══════════════════════════════════════╗");
            log.info("║    !!! SECURITY BREACH DETECTED !!!   ║");
            log.info("║    Initializing Defense Protocol...   ║");
            log.info("║    Threat Level: CRITICAL             ║");
            log.info("╚═══════════════════════════════════════╝");
            log.info("💀 [킬구형]: 망했다! 보안 시스템이 우리를 감지했다!");
            log.info("💀 [킬구형]: 프로토콜이 완료되기 전에 중단시켜야 한다!");
            log.info("💀 [킬구형]: JobOperator.stop() 명령으로 즉시 중단하라!");
            log.info("⚠️ [시스템] 30초 내에 중단하지 않으면 모든 흔적이 남는다!");

            log.info("\n====================== [카운트다운] ======================");
            for (int i = 30; i > 0; i--) {
                if (i % 5 == 0) {
                    log.info("⚠️ 남은 시간: {}초... 빨리 작업을 중단하라!", i);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }

            log.info("🔴 [경고]: 시간 초과! 모든 보안 프로토콜이 실행됩니다!");
            log.info("🔴 [보안 시스템]: 침입자 위치 특정! 추적 시작!");
        }

        return items.get(index++);
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        super.open(executionContext);

        if (executionContext.containsKey(getExecutionContextKey("started"))) {
            log.info("====================== [비상 상황] ======================");
            log.info("⚠️⚠️⚠️ [시스템 경고] 보안 프로토콜 재가동 감지!");
            log.info("💀 [킬구형]: 뭐라고?! 중단했던 보안 시스템이 다시 살아났다!\n");
        } else {
            log.info("====================== [작전 시작] ======================");
            log.info("💀 [킬구형]: 서버에 침투 시도 중... 조용히 움직여라.\n");
        }
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.put(getExecutionContextKey("started"), true);
        super.update(executionContext);
    }

    @Override
    protected void doOpen() throws Exception {
    }

    @Override
    protected void doClose() throws Exception {
    }

    @Override
    protected void jumpToItem(int itemIndex) {
        this.index = itemIndex;
        log.info("🚀🚀🚀 [시스템]: 점프 실행! 프로토콜 #" + (itemIndex + 1) + "로 이동.");
    }
}