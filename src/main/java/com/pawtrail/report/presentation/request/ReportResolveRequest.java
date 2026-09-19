package com.pawtrail.report.presentation.request;

import com.pawtrail.report.application.dto.input.ReportResolveInput;
import com.pawtrail.report.domain.enums.ReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 관리자 처리 요청입니다.
 *
 * 처리 결과가 PENDING 이면 형식은 맞으니 여기를 지나고 서비스가 400 으로 돌려보냅니다.
 * 메모는 승인 · 반려 둘 다 필수입니다. 결과 알림 본문에 그대로 실려 사용자가 읽습니다.
 */
public record ReportResolveRequest(

        @NotNull(message = "처리 결과를 골라 주세요")
        ReportStatus status,

        @NotBlank(message = "처리 메모를 적어 주세요")
        @Size(max = 500, message = "처리 메모는 500자까지 쓸 수 있습니다")
        String memo
) {

    public ReportResolveInput toInput() {
        return new ReportResolveInput(status, memo);
    }
}
