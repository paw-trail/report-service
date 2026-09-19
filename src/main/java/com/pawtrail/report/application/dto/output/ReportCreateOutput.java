package com.pawtrail.report.application.dto.output;

import java.util.UUID;

/**
 * 제보를 받은 결과입니다.
 *
 * 식별자 하나만 돌려줍니다. 화면이 알 방법이 없는 값이 이것뿐입니다.
 * 상태는 언제나 처리 전(PENDING)이라 싣지 않습니다.
 *
 * 카드 전체를 돌려주지 않는 이유는 제출 때 place 를 부르지 않아 장소 이름이 비기 때문입니다.
 * 그 카드는 고객센터 목록에서 이름까지 채워 보여줍니다.
 *
 * @param reportId 새로 만든 제보의 식별자입니다.
 */
public record ReportCreateOutput(UUID reportId) {
}
