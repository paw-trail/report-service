package com.pawtrail.report.domain.model;

import com.pawtrail.common.entity.BaseEntity;
import com.pawtrail.report.domain.enums.ReportStatus;
import com.pawtrail.report.domain.enums.ReportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * 사용자가 올린 제보 한 건입니다.
 *
 * 장소 정보가 틀렸다는 제보와 불건전 후기 신고를 한 표에 담습니다.
 * 흐름이 같기 때문입니다. 사용자가 올리고, 관리자가 승인 · 반려하고, 결과를 알림으로 받습니다.
 *
 * 승인해도 장소 · 조건 값은 여기서 바뀌지 않습니다.
 * report 는 place · policy 를 고칠 권한이 없고, 실제 정정은 관리자가 그쪽 관리자 API 로 먼저 합니다.
 *
 * 후기 신고도 장소를 가집니다. 그 후기가 달린 장소입니다.
 * 관리자가 신고된 후기를 볼 수 있는 자리가 장소 상세의 후기 카드뿐이라
 * 장소가 없으면 무엇을 신고했는지 확인할 길이 없습니다.
 */
@Entity
@Table(name = "report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "place_id", nullable = false)
    private UUID placeId;

    @Column(name = "target_review_id")
    private UUID targetReviewId;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 24)
    private ReportType reportType;

    @Column(name = "field_name", length = 40)
    private String fieldName;

    @Column(name = "reported_value", length = 500)
    private String reportedValue;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "visited_at")
    private LocalDate visitedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReportStatus status;

    @Column(name = "reviewed_by", length = 45)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(length = 500)
    private String memo;

    /**
     * 새 제보를 만듭니다. 상태는 처리 전(PENDING)입니다.
     *
     * 유형별 칸 규칙을 한 번 더 봅니다.
     * 서비스가 먼저 걸러 400 으로 돌려보내므로 여기까지 오면 우리 코드가 잘못 부른 것이라
     * IllegalArgumentException 을 던지고, 그러면 500 이 나갑니다.
     * DB 의 CHECK 도 후기 식별자 쪽을 막지만 나머지 칸은 거기서 못 막아 이 검사가 마지막 방어선입니다.
     *
     * @throws IllegalArgumentException 필수 값이 없거나 유형과 맞지 않는 칸이 있으면
     */
    public static Report submit(UUID accountId,
                                UUID placeId,
                                ReportType reportType,
                                String fieldName,
                                String reportedValue,
                                String content,
                                LocalDate visitedAt,
                                UUID targetReviewId) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(placeId, "placeId");
        Objects.requireNonNull(reportType, "reportType");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content 가 비어 있습니다");
        }

        List<String> violations =
                reportType.violations(fieldName, reportedValue, visitedAt, targetReviewId);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(reportType + " 칸 규칙 위반: " + violations);
        }

        Report report = new Report();
        report.accountId = accountId;
        report.placeId = placeId;
        report.reportType = reportType;
        report.fieldName = fieldName;
        report.reportedValue = reportedValue;
        report.content = content;
        report.visitedAt = visitedAt;
        report.targetReviewId = targetReviewId;
        report.status = ReportStatus.PENDING;
        return report;
    }
}
