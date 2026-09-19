package com.pawtrail.report.application.dto.input;

import static org.assertj.core.api.Assertions.assertThat;

import com.pawtrail.report.domain.enums.ReportType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 비어 있는 칸 이름 · 맞는 값을 안 온 것으로 바꾸는지 검사합니다.
 */
class ReportCreateInputTest {

    private static final UUID PLACE = UUID.fromString("01999999-0000-7000-8000-00000000aaaa");

    @Test
    @DisplayName("빈 문자열과 공백만 있는 값은 안 온 것이 된다")
    void 빈_문자열() {
        ReportCreateInput input =
                new ReportCreateInput(PLACE, ReportType.CLOSED, "", "   ", "문을 닫았어요", null, null);

        assertThat(input.fieldName()).isNull();
        assertThat(input.reportedValue()).isNull();
        assertThat(ReportType.CLOSED.violations(
                input.fieldName(), input.reportedValue(), input.visitedAt(), input.targetReviewId())).isEmpty();
    }

    @Test
    @DisplayName("값이 있으면 앞뒤 공백만 지우고, 본문은 손대지 않는다")
    void 앞뒤_공백() {
        ReportCreateInput input = new ReportCreateInput(
                PLACE, ReportType.INFO_WRONG, " tel ", " 02-1234-5678 ", "  번호가 달라요  ", null, null);

        assertThat(input.fieldName()).isEqualTo("tel");
        assertThat(input.reportedValue()).isEqualTo("02-1234-5678");
        assertThat(input.content()).isEqualTo("  번호가 달라요  ");
    }
}
