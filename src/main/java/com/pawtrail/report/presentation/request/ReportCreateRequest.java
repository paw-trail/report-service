package com.pawtrail.report.presentation.request;

import com.pawtrail.report.application.dto.input.ReportCreateInput;
import com.pawtrail.report.domain.enums.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 제보 등록 요청입니다.
 *
 * 누가 올리는지는 받지 않습니다.
 * 게이트웨이가 넣은 X-User-Id 를 컨트롤러가 @CurrentUser 로 꺼내 씁니다.
 *
 * 여기서는 형식만 봅니다. 어긋나면 칸 목록이 응답(data)에 실려 화면이 그 칸에 문구를 띄울 수 있습니다.
 * 유형마다 어떤 칸을 받는지는 서비스가 봅니다. 한 칸만 봐서는 판단할 수 없는 규칙이라서입니다.
 *
 * 폭은 컬럼과 같습니다. 여기서 안 막으면 DB 가 막는데, 그때는 어느 칸이 문제인지 응답에 안 실립니다.
 *
 * reportType 에 모르는 값이 오면 여기까지 오지 못하고 본문을 읽는 단계에서 400 이 납니다.
 */
public record ReportCreateRequest(

        @NotNull(message = "장소를 골라 주세요")
        UUID placeId,

        @NotNull(message = "제보 유형을 골라 주세요")
        ReportType reportType,

        @Size(max = 40, message = "칸 이름은 40자까지 쓸 수 있습니다")
        String fieldName,

        @Size(max = 500, message = "맞는 값은 500자까지 쓸 수 있습니다")
        String reportedValue,

        @NotBlank(message = "내용을 적어 주세요")
        @Size(max = 1000, message = "내용은 1000자까지 쓸 수 있습니다")
        String content,

        // 오늘까지만 받음 — 다녀온 날이라 미래일 수 없음
        // 서버 시계의 날짜로 보며 컨테이너는 TZ=Asia/Seoul 이라 서울 날짜임
        @PastOrPresent(message = "방문일은 오늘까지 고를 수 있습니다")
        LocalDate visitedAt,

        UUID targetReviewId
) {

    public ReportCreateInput toInput() {
        return new ReportCreateInput(
                placeId, reportType, fieldName, reportedValue, content, visitedAt, targetReviewId);
    }
}
