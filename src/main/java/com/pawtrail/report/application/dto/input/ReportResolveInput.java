package com.pawtrail.report.application.dto.input;

import com.pawtrail.report.domain.enums.ReportStatus;

/**
 * 관리자 처리에 필요한 값입니다.
 *
 * 누가 처리하는지는 담지 않습니다. 게이트웨이가 넣은 X-User-Id 를 컨트롤러가 따로 넘깁니다.
 *
 * @param status 처리 결과입니다. ACCEPTED 또는 REJECTED 여야 합니다.
 * @param memo   처리 메모입니다. 결과 알림 본문에 그대로 실립니다.
 */
public record ReportResolveInput(ReportStatus status, String memo) {
}
