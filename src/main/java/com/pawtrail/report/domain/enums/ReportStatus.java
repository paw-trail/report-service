package com.pawtrail.report.domain.enums;

/**
 * 제보의 처리 상태입니다.
 *
 * 처음에는 PENDING 이고 관리자가 한 번 처리하면 ACCEPTED 나 REJECTED 가 됩니다.
 * 처리한 제보는 다시 처리하지 않습니다.
 * 결과가 report.resolved 로 한 번 나가므로 두 번 처리하면 알림이 뒤집힙니다.
 *
 * 이름을 수집 대기(place_pending_update.status)와 같은 PENDING 으로 둡니다.
 * 관리자가 보는 "처리 전" 이 두 화면에서 같은 말이 되게 하기 위해서입니다.
 */
public enum ReportStatus {

    PENDING,
    ACCEPTED,
    REJECTED
}
