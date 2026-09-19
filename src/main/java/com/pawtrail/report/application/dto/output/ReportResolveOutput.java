package com.pawtrail.report.application.dto.output;

import com.pawtrail.report.domain.enums.ReportStatus;
import com.pawtrail.report.domain.model.Report;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자가 처리한 결과입니다.
 *
 * 처리로 바뀐 칸만 돌려줍니다. 관리자 화면이 카드에서 바꿀 것이 이것뿐입니다.
 * 장소 이름과 제보자는 목록에서 이미 받아 두었으므로 싣지 않고,
 * 처리하면서 place · user 를 부르지도 않습니다.
 *
 * @param reportId   처리한 제보입니다.
 * @param status     ACCEPTED 또는 REJECTED 입니다.
 * @param memo       처리 메모입니다.
 * @param reviewedBy 처리한 관리자의 계정 식별자입니다.
 * @param reviewedAt 처리한 시각입니다.
 */
public record ReportResolveOutput(UUID reportId,
                                  ReportStatus status,
                                  String memo,
                                  String reviewedBy,
                                  LocalDateTime reviewedAt) {

    public static ReportResolveOutput from(Report report) {
        return new ReportResolveOutput(
                report.getId(),
                report.getStatus(),
                report.getMemo(),
                report.getReviewedBy(),
                report.getReviewedAt());
    }
}
