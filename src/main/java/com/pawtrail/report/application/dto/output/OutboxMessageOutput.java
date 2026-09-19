package com.pawtrail.report.application.dto.output;

import com.pawtrail.common.message.outbox.OutboxMessage;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 발행이 멈춘 이벤트 한 건입니다. 관리자 outbox 목록의 원소입니다.
 *
 * policy 의 관리자 outbox 와 같은 여덟 칸입니다. 서비스가 달라도 같은 화면으로 볼 수 있게 맞췄습니다.
 * 본문(payload)은 싣지 않습니다. 개인 메모가 들어 있어 목록에 늘어놓을 값이 아닙니다.
 *
 * @param id            다시 보낼 때 넘기는 식별자입니다.
 * @param eventId       이벤트 식별자입니다. 받는 쪽이 중복을 거를 때 씁니다.
 * @param topic         보낼 토픽입니다. report 는 report.resolved 하나입니다.
 * @param aggregateType 이벤트의 주인 종류입니다. report 는 Report 입니다.
 * @param aggregateId   이벤트의 주인입니다. 제보 식별자입니다.
 * @param createdAt     기록한 시각입니다.
 * @param retryCount    지금까지 다시 보낸 횟수입니다.
 * @param lastError     마지막 실패 이유입니다.
 */
public record OutboxMessageOutput(UUID id,
                                  UUID eventId,
                                  String topic,
                                  String aggregateType,
                                  String aggregateId,
                                  LocalDateTime createdAt,
                                  int retryCount,
                                  String lastError) {

    public static OutboxMessageOutput from(OutboxMessage message) {
        return new OutboxMessageOutput(
                message.getId(),
                message.getEventId(),
                message.getTopic(),
                message.getAggregateType(),
                message.getAggregateId(),
                message.getCreatedAt(),
                message.getRetryCount(),
                message.getLastError());
    }
}
