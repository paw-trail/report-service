package com.pawtrail.report.domain.enums;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 제보 유형입니다.
 *
 * 장소 대상 4종과 후기 대상 1종이며, 유형마다 받는 칸이 다릅니다.
 * 그 규칙을 이 열거형이 들고 있어 서비스와 엔티티가 같은 기준으로 봅니다.
 *
 * <pre>
 * 유형                 fieldName   reportedValue      visitedAt   targetReviewId
 * INFO_WRONG           필수         선택 (맞는 값)       선택          ✕
 * CONDITION_WRONG      선택         선택 (맞는 값)       선택          ✕
 * PLACE_MERGED_WRONG   ✕           필수 (소스 코드)     ✕            ✕
 * CLOSED               ✕           ✕                  선택          ✕
 * REVIEW_ABUSE         ✕           ✕                  ✕            필수
 * </pre>
 *
 * 조건 제보의 칸 이름을 선택으로 둔 것은 사용자가 어느 조건 때문에 거절당했는지
 * 모를 수 있기 때문입니다. 장소 정보 제보는 무엇이 틀렸는지를 사용자가 늘 압니다.
 *
 * 칸 이름과 소스 코드가 어떤 값이어야 하는지는 여기서 보지 않습니다.
 * 장소 칸은 place 가, 조건 칸은 policy 가, 소스는 place 가 정하는 목록이라
 * report 가 그것을 들고 있으면 저쪽이 칸을 늘릴 때마다 report 도 고쳐 배포해야 합니다.
 * 목록은 화면의 드롭다운이 가지고, 여기서는 칸이 있는지 없는지만 봅니다.
 */
public enum ReportType {

    INFO_WRONG(Rule.REQUIRED, Rule.OPTIONAL, Rule.OPTIONAL, Rule.NONE),
    CONDITION_WRONG(Rule.OPTIONAL, Rule.OPTIONAL, Rule.OPTIONAL, Rule.NONE),
    PLACE_MERGED_WRONG(Rule.NONE, Rule.REQUIRED, Rule.NONE, Rule.NONE),
    CLOSED(Rule.NONE, Rule.NONE, Rule.OPTIONAL, Rule.NONE),
    REVIEW_ABUSE(Rule.NONE, Rule.NONE, Rule.NONE, Rule.REQUIRED);

    private final Rule fieldName;
    private final Rule reportedValue;
    private final Rule visitedAt;
    private final Rule targetReviewId;

    ReportType(Rule fieldName, Rule reportedValue, Rule visitedAt, Rule targetReviewId) {
        this.fieldName = fieldName;
        this.reportedValue = reportedValue;
        this.visitedAt = visitedAt;
        this.targetReviewId = targetReviewId;
    }

    /**
     * 이 유형의 칸 규칙에 어긋나는 자리를 찾습니다.
     *
     * 어긋난 칸마다 한 줄씩 돌려주고, 맞으면 빈 목록입니다.
     * 응답에는 싣지 않고 로그에만 남깁니다.
     * 폼이 유형마다 칸을 따로 그려서 이 위반은 화면 밖 호출이나 화면 실수에서만 나오기 때문입니다.
     *
     * 빈 문자열은 부르는 쪽이 미리 null 로 바꿔 넘깁니다.
     * 여기서는 값이 있는지만 봅니다.
     */
    public List<String> violations(String fieldName,
                                   String reportedValue,
                                   LocalDate visitedAt,
                                   UUID targetReviewId) {
        List<String> result = new ArrayList<>();
        check(result, "fieldName", this.fieldName, fieldName != null);
        check(result, "reportedValue", this.reportedValue, reportedValue != null);
        check(result, "visitedAt", this.visitedAt, visitedAt != null);
        check(result, "targetReviewId", this.targetReviewId, targetReviewId != null);
        return result;
    }

    private static void check(List<String> result, String name, Rule rule, boolean present) {
        if (rule == Rule.REQUIRED && !present) {
            result.add(name + " 이(가) 필요함");
        }
        if (rule == Rule.NONE && present) {
            result.add(name + " 은(는) 이 유형에서 받지 않음");
        }
    }

    /**
     * 칸 하나를 받는 방식입니다.
     */
    public enum Rule {
        REQUIRED,
        OPTIONAL,
        NONE
    }
}
