package com.pawtrail.report.application.dto.input;

import com.pawtrail.report.domain.enums.ReportType;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 제보 등록에 필요한 값입니다.
 *
 * 누가 올리는지는 담지 않습니다. 게이트웨이가 넣은 X-User-Id 를 컨트롤러가 따로 넘깁니다.
 *
 * 칸 이름과 맞는 값은 비어 있으면 안 온 것으로 봅니다.
 * 폼이 손대지 않은 입력칸을 빈 문자열로 보내는 일이 흔한데,
 * 그것을 값으로 보면 받지 않는 칸에 값이 왔다며 400 이 나가고 DB 에도 빈 문자열이 남습니다.
 * 앞뒤 공백은 지워서 담습니다.
 *
 * 본문은 손대지 않습니다. 비어 있는 본문은 요청 검증(@NotBlank)이 이미 막았습니다.
 */
public record ReportCreateInput(UUID placeId,
                                ReportType reportType,
                                String fieldName,
                                String reportedValue,
                                String content,
                                LocalDate visitedAt,
                                UUID targetReviewId) {

    public ReportCreateInput {
        fieldName = blankToNull(fieldName);
        reportedValue = blankToNull(reportedValue);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
