package com.pawtrail.report.domain.event.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pawtrail.report.domain.enums.ReportStatus;
import com.pawtrail.report.domain.enums.ReportType;
import com.pawtrail.report.domain.model.Report;
import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * report.resolved 에 무엇이 실리는지 검사합니다. notification 과의 약속입니다.
 */
class ReportResolvedEventTest {

    private static final UUID REPORT = UUID.fromString("01999999-0000-7000-8000-000000000101");
    private static final UUID ACCOUNT = UUID.fromString("01999999-0000-7000-8000-000000000001");
    private static final UUID PLACE = UUID.fromString("01999999-0000-7000-8000-00000000aaaa");
    private static final UUID REVIEW = UUID.fromString("01999999-0000-7000-8000-00000000bbbb");

    @Test
    @DisplayName("처리한 제보에서 여섯 칸을 옮기고, 토픽 · 주인 · 키가 정해져 있다")
    void 실리는_것() throws Exception {
        Report report = Report.submit(ACCOUNT, PLACE, ReportType.REVIEW_ABUSE, null, null, "욕설이 있어요", null, REVIEW);
        setId(report, REPORT);
        report.resolve(ReportStatus.ACCEPTED, "후기를 지웠습니다", "01999999-0000-7000-8000-00000000a0a0");

        ReportResolvedEvent event = ReportResolvedEvent.from(report);

        assertThat(event.reportId()).isEqualTo(REPORT);
        assertThat(event.accountId()).isEqualTo(ACCOUNT);
        assertThat(event.reportType()).isEqualTo(ReportType.REVIEW_ABUSE);
        assertThat(event.status()).isEqualTo(ReportStatus.ACCEPTED);
        assertThat(event.memo()).isEqualTo("후기를 지웠습니다");
        assertThat(event.placeId()).isEqualTo(PLACE);
        assertThat(event.getTopic()).isEqualTo("report.resolved");
        assertThat(event.getAggregateType()).isEqualTo("Report");
        assertThat(event.getAggregateId()).isEqualTo(REPORT.toString());
    }

    @Test
    @DisplayName("처리 전 제보는 내보내지 않는다")
    void 처리_전() {
        assertThatThrownBy(() -> new ReportResolvedEvent(
                REPORT, ACCOUNT, ReportType.CLOSED, ReportStatus.PENDING, null, PLACE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static void setId(Report report, UUID id) throws Exception {
        Field field = Report.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(report, id);
    }
}
