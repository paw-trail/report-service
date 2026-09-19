package com.pawtrail.report.application.dto.output;

import com.pawtrail.report.domain.enums.ReportStatus;
import com.pawtrail.report.domain.enums.ReportType;
import com.pawtrail.report.domain.model.Report;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 내 제보 목록(마이페이지 고객센터)의 카드 하나입니다.
 *
 * 코드값(reportType · fieldName · status)은 그대로 내보내고 사람이 읽는 이름은 화면이 붙입니다.
 * 칸 이름의 목록을 report 가 들고 있지 않기 때문입니다.
 *
 * placeName 은 비어 있을 수 있습니다.
 * place 를 못 불렀거나 그 장소가 없으면 이름만 비우고 카드는 그대로 냅니다.
 * 화면은 "장소 이름을 불러오지 못했습니다" 처럼 안내하면 됩니다.
 *
 * memo · reviewedAt 은 관리자가 처리하기 전에는 비어 있습니다.
 * 처리한 관리자(reviewedBy)는 싣지 않습니다. 사용자에게 필요한 값이 아닙니다.
 */
public record ReportCardOutput(UUID reportId,
                               ReportType reportType,
                               UUID placeId,
                               String placeName,
                               UUID targetReviewId,
                               String fieldName,
                               String reportedValue,
                               String content,
                               LocalDate visitedAt,
                               ReportStatus status,
                               String memo,
                               LocalDateTime reviewedAt,
                               LocalDateTime createdAt) {

    public static ReportCardOutput of(Report report, String placeName) {
        return new ReportCardOutput(
                report.getId(),
                report.getReportType(),
                report.getPlaceId(),
                placeName,
                report.getTargetReviewId(),
                report.getFieldName(),
                report.getReportedValue(),
                report.getContent(),
                report.getVisitedAt(),
                report.getStatus(),
                report.getMemo(),
                report.getReviewedAt(),
                report.getCreatedAt());
    }
}
