package com.pawtrail.report.application.service;

import com.pawtrail.common.exception.CustomException;
import com.pawtrail.common.message.outbox.OutboxPublisher;
import com.pawtrail.common.message.outbox.OutboxRepository;
import com.pawtrail.common.response.PageResponse;
import com.pawtrail.report.application.dto.output.OutboxMessageOutput;
import com.pawtrail.report.domain.exception.ReportErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 발행이 끝내 실패해 멈춘 이벤트를 관리자가 보고 다시 보내는 일을 맡습니다.
 *
 * policy 의 AdminOutboxService 와 같습니다. 재시도를 다 쓴 행이 조용히 쌓이지 않게
 * 사람이 보고 손으로 다시 보낼 길을 서비스마다 둡니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOutboxService {

    private final OutboxRepository outboxRepository;
    private final OutboxPublisher outboxPublisher;

    /**
     * 멈춘 이벤트를 오래된 것부터 한 쪽 돌려줍니다. 아직 재시도 중인 것은 나오지 않습니다.
     */
    @Transactional(readOnly = true)
    public PageResponse<OutboxMessageOutput> findGivenUp(Pageable pageable) {
        return PageResponse.from(
                outboxRepository.findGivenUpMessages(pageable), OutboxMessageOutput::from);
    }

    /**
     * 한 건을 다시 보냅니다. 실패하면 성공으로 응답하지 않습니다.
     */
    @Transactional
    public void republish(UUID outboxId) {
        boolean published = outboxPublisher.publish(outboxId);
        if (!published) {
            log.error("관리자 재발행에 실패했습니다. outboxId={}", outboxId);
            throw new CustomException(ReportErrorCode.OUTBOX_REPUBLISH_FAILED);
        }
        log.info("관리자가 이벤트를 다시 발행했습니다. outboxId={}", outboxId);
    }
}
