package com.pawtrail.report.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pawtrail.report.domain.enums.ReportStatus;
import com.pawtrail.report.domain.enums.ReportType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 제보 처리의 마지막 방어선을 검사합니다.
 *
 * 서비스가 400 · 409 로 먼저 거르므로 여기 걸리는 것은 우리 코드의 잘못입니다.
 */
class ReportTest {

    private static final UUID ACCOUNT = UUID.fromString("01999999-0000-7000-8000-000000000001");
    private static final UUID PLACE = UUID.fromString("01999999-0000-7000-8000-00000000aaaa");
    private static final String ADMIN = "01999999-0000-7000-8000-00000000a0a0";

    @Test
    @DisplayName("처리 전 제보를 승인하면 상태 · 메모 · 처리자 · 처리 시각이 함께 채워진다")
    void 승인() {
        Report report = closed();

        report.resolve(ReportStatus.ACCEPTED, "폐업을 확인했습니다", ADMIN);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.ACCEPTED);
        assertThat(report.getMemo()).isEqualTo("폐업을 확인했습니다");
        assertThat(report.getReviewedBy()).isEqualTo(ADMIN);
        assertThat(report.getReviewedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 처리한 제보는 다시 처리하지 않는다")
    void 두_번() {
        Report report = closed();
        report.resolve(ReportStatus.REJECTED, "확인되지 않았습니다", ADMIN);

        assertThatThrownBy(() -> report.resolve(ReportStatus.ACCEPTED, "다시", ADMIN))
                .isInstanceOf(IllegalStateException.class);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.REJECTED);
    }

    @Test
    @DisplayName("처리 결과가 PENDING 이거나 메모 · 처리자가 비면 거절한다")
    void 잘못_부름() {
        assertThatThrownBy(() -> closed().resolve(ReportStatus.PENDING, "메모", ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> closed().resolve(ReportStatus.ACCEPTED, "  ", ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> closed().resolve(ReportStatus.ACCEPTED, "메모", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Report closed() {
        return Report.submit(ACCOUNT, PLACE, ReportType.CLOSED, null, null, "문을 닫았어요", null, null);
    }
}
