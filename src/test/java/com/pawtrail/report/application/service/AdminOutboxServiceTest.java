package com.pawtrail.report.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pawtrail.common.exception.CustomException;
import com.pawtrail.common.message.outbox.OutboxPublisher;
import com.pawtrail.common.message.outbox.OutboxRepository;
import com.pawtrail.report.domain.exception.ReportErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 관리자 재발행이 실패를 성공으로 덮지 않는지 검사합니다.
 */
@ExtendWith(MockitoExtension.class)
class AdminOutboxServiceTest {

    private static final UUID OUTBOX = UUID.fromString("01999999-0000-7000-8000-0000000000f1");

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OutboxPublisher outboxPublisher;

    @InjectMocks
    private AdminOutboxService adminOutboxService;

    @Test
    @DisplayName("다시 보내는 데 성공하면 조용히 끝난다")
    void 성공() {
        when(outboxPublisher.publish(OUTBOX)).thenReturn(true);

        adminOutboxService.republish(OUTBOX);

        verify(outboxPublisher).publish(OUTBOX);
    }

    @Test
    @DisplayName("또 실패하면 500 OUTBOX_REPUBLISH_FAILED 로 알린다")
    void 실패() {
        when(outboxPublisher.publish(OUTBOX)).thenReturn(false);

        assertThatThrownBy(() -> adminOutboxService.republish(OUTBOX))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ReportErrorCode.OUTBOX_REPUBLISH_FAILED));
    }
}
