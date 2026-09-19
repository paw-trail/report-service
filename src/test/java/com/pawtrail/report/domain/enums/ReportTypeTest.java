package com.pawtrail.report.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 유형별 칸 규칙을 검사합니다.
 *
 * 규칙표 다섯 줄을 필수 칸이 빈 경우 · 받지 않는 칸에 값이 온 경우 · 맞는 경우로 나눠 봅니다.
 */
class ReportTypeTest {

    private static final UUID REVIEW = UUID.fromString("01999999-0000-7000-8000-00000000bbbb");
    private static final LocalDate DAY = LocalDate.of(2026, 9, 15);

    @Test
    @DisplayName("필수 칸이 비면 한 줄씩 걸린다")
    void 필수_칸() {
        assertThat(ReportType.INFO_WRONG.violations(null, null, null, null)).hasSize(1);
        assertThat(ReportType.PLACE_MERGED_WRONG.violations(null, null, null, null)).hasSize(1);
        assertThat(ReportType.REVIEW_ABUSE.violations(null, null, null, null)).hasSize(1);
    }

    @Test
    @DisplayName("받지 않는 칸에 값이 오면 걸린다")
    void 받지_않는_칸() {
        assertThat(ReportType.CLOSED.violations("tel", null, null, null)).hasSize(1);
        assertThat(ReportType.CLOSED.violations(null, "문 닫음", null, null)).hasSize(1);
        assertThat(ReportType.PLACE_MERGED_WRONG.violations("tel", "GOCAMPING", DAY, null)).hasSize(2);
        assertThat(ReportType.REVIEW_ABUSE.violations(null, null, DAY, REVIEW)).hasSize(1);
        assertThat(ReportType.INFO_WRONG.violations("tel", null, null, REVIEW)).hasSize(1);
        assertThat(ReportType.CONDITION_WRONG.violations(null, null, null, REVIEW)).hasSize(1);
    }

    @Test
    @DisplayName("규칙대로면 비어 있다 — 선택 칸은 있어도 없어도 된다")
    void 규칙대로() {
        assertThat(ReportType.INFO_WRONG.violations("tel", null, null, null)).isEmpty();
        assertThat(ReportType.INFO_WRONG.violations("tel", "02-1234-5678", DAY, null)).isEmpty();
        assertThat(ReportType.CONDITION_WRONG.violations(null, null, null, null)).isEmpty();
        assertThat(ReportType.CONDITION_WRONG.violations("maxWeightKg", "5kg 이하", DAY, null)).isEmpty();
        assertThat(ReportType.PLACE_MERGED_WRONG.violations(null, "GOCAMPING", null, null)).isEmpty();
        assertThat(ReportType.CLOSED.violations(null, null, null, null)).isEmpty();
        assertThat(ReportType.CLOSED.violations(null, null, DAY, null)).isEmpty();
        assertThat(ReportType.REVIEW_ABUSE.violations(null, null, null, REVIEW)).isEmpty();
    }
}
