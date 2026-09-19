package com.pawtrail.report.application.dto.output;

import com.pawtrail.report.domain.enums.ReportStatus;
import com.pawtrail.report.domain.enums.ReportType;
import com.pawtrail.report.domain.model.Report;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자 제보 목록의 카드 하나입니다.
 *
 * 내 제보 카드와 칸이 같고 제보자(reporter) · 처리자(reviewedBy)가 더 붙습니다.
 *
 * 제보자는 늘 싣습니다. 계정 식별자는 우리 값이라 언제나 있고,
 * 닉네임 · 사진만 user 에서 받아 채웁니다. 못 받았거나 탈퇴한 사람이면 그 둘이 비어 있습니다.
 * 화면은 "탈퇴했거나 불러오지 못한 사용자" 처럼 안내하면 됩니다.
 *
 * placeName 이 비는 경우는 내 제보 카드와 같습니다.
 */
public record AdminReportCardOutput(UUID reportId,
                                    ReportType reportType,
                                    UUID placeId,
                                    String placeName,
                                    UUID targetReviewId,
                                    String fieldName,
                                    String reportedValue,
                                    String content,
                                    LocalDate visitedAt,
                                    ReporterOutput reporter,
                                    ReportStatus status,
                                    String memo,
                                    String reviewedBy,
                                    LocalDateTime reviewedAt,
                                    LocalDateTime createdAt) {

    /**
     * 제보한 사람입니다.
     *
     * 후기 목록의 작성자(author)와 같은 묶음 모양입니다.
     *
     * @param accountId       계정 식별자입니다. 늘 있습니다.
     * @param nickname        닉네임입니다. 못 받았으면 비어 있습니다.
     * @param profileImageUrl 프로필 사진입니다. 없거나 못 받았으면 비어 있습니다.
     */
    public record ReporterOutput(UUID accountId, String nickname, String profileImageUrl) {
    }

    public static AdminReportCardOutput of(Report report, String placeName, ReporterOutput reporter) {
        return new AdminReportCardOutput(
                report.getId(),
                report.getReportType(),
                report.getPlaceId(),
                placeName,
                report.getTargetReviewId(),
                report.getFieldName(),
                report.getReportedValue(),
                report.getContent(),
                report.getVisitedAt(),
                reporter,
                report.getStatus(),
                report.getMemo(),
                report.getReviewedBy(),
                report.getReviewedAt(),
                report.getCreatedAt());
    }
}
