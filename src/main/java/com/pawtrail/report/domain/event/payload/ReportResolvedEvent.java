package com.pawtrail.report.domain.event.payload;

import com.pawtrail.common.message.DomainEvent;
import com.pawtrail.report.domain.enums.ReportStatus;
import com.pawtrail.report.domain.enums.ReportType;
import com.pawtrail.report.domain.model.Report;
import java.util.UUID;

/**
 * 관리자가 제보를 처리했다는 이벤트입니다. notification 이 받아 제보한 사람에게 알립니다.
 *
 * 승인과 반려 둘 다 나갑니다. 반려도 알려야 사용자가 제보가 읽혔다는 것을 압니다.
 * 제보 하나에 한 번만 나갑니다. 처리한 제보는 다시 처리하지 않기 때문입니다.
 *
 * 장소 식별자를 싣습니다.
 * notification 이 알림 문구에 장소 이름을 넣고 눌렀을 때 그 장소로 보내는 데 씁니다.
 * 이름은 싣지 않습니다. 이름은 place 가 바꿀 수 있어 알림을 만들 때 place 에서 받는 편이 맞습니다.
 *
 * 후기 식별자는 싣지 않습니다. 후기 신고를 승인했다면 그 후기는 이미 지워져 보낼 곳이 없습니다.
 *
 * 메시지 키는 제보 식별자입니다. 같은 제보의 이벤트가 한 파티션에 모입니다.
 *
 * @param reportId   처리한 제보입니다.
 * @param accountId  제보한 사람입니다. 알림을 받을 사람입니다.
 * @param reportType 제보 유형입니다. 알림 문구가 이것으로 갈립니다.
 * @param status     ACCEPTED 또는 REJECTED 입니다.
 * @param memo       관리자가 남긴 처리 메모입니다. 알림 본문에 그대로 실립니다.
 * @param placeId    제보 대상 장소입니다. 후기 신고는 그 후기가 달린 장소입니다.
 */
public record ReportResolvedEvent(
        UUID reportId,
        UUID accountId,
        ReportType reportType,
        ReportStatus status,
        String memo,
        UUID placeId
) implements DomainEvent {

    public ReportResolvedEvent {
        if (reportId == null || accountId == null || reportType == null || placeId == null) {
            throw new IllegalArgumentException("reportId · accountId · reportType · placeId 는 필수입니다.");
        }
        if (status != ReportStatus.ACCEPTED && status != ReportStatus.REJECTED) {
            throw new IllegalArgumentException("처리된 제보만 내보냅니다: " + status);
        }
    }

    public static ReportResolvedEvent from(Report report) {
        return new ReportResolvedEvent(
                report.getId(),
                report.getAccountId(),
                report.getReportType(),
                report.getStatus(),
                report.getMemo(),
                report.getPlaceId());
    }

    @Override
    public String getTopic() {
        return "report.resolved";
    }

    @Override
    public String getAggregateType() {
        return "Report";
    }

    @Override
    public String getAggregateId() {
        return reportId.toString();
    }
}
