package com.pawtrail.report.domain.exception;

import com.pawtrail.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * report 서비스가 내는 오류입니다.
 *
 * 상수 이름이 곧 응답의 code 이자 화면과의 약속입니다.
 * 이름을 바꾸면 화면이 그 오류를 못 알아보므로 바꾸지 않습니다.
 *
 * 입력 형식이 틀린 것은 여기 두지 않고 공통의 VALIDATION_FAILED 를 씁니다.
 * 유형별 칸 규칙을 어긴 것도 같은 코드로 나갑니다.
 */
@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements ErrorCode {

    // 그런 제보가 없음 — 관리자 처리에서만 남
    //
    // * 탈퇴한 사람의 제보는 행째 지워지므로 관리자가 보고 있던 카드가 사라질 수 있음
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "제보를 찾을 수 없습니다."),

    // 같은 사람이 처리 중인 같은 제보를 또 올림
    //
    // 같은 제보 = 같은 장소 · 같은 유형 · 같은 칸 · 같은 후기
    // 처리가 끝나면(승인 · 반려) 다시 올릴 수 있음
    //
    // * 멱등 성공(이미 있는 것을 돌려줌)으로 두지 않은 이유
    //   제보는 본문을 싣는 동작이라 새로 쓴 본문이 소리 없이 버려지게 됨
    //   즐겨찾기처럼 "담긴 상태" 를 만드는 동작과 다름
    REPORT_ALREADY_PENDING(HttpStatus.CONFLICT, "처리 중인 같은 제보가 있습니다."),

    // 오늘 올릴 수 있는 제보를 다 씀
    //
    // * 도배를 막는 장치임
    //   계정 정지가 없어서 관리자가 하나씩 반려하는 것 말고는 막을 수단이 없음
    // * 상한은 config 의 app.report.daily-limit 이고 없으면 20
    // * 저장된 제보만 셈 — 거절된 요청은 세지 않음
    REPORT_DAILY_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "오늘 올릴 수 있는 제보를 다 썼습니다."),

    // 이미 처리한 제보를 또 처리함
    //
    // * 결과는 report.resolved 로 한 번만 나감 — 두 번 처리하면 사용자가 받는 알림이 뒤집힘
    // * 두 관리자가 같은 카드를 동시에 눌렀을 때 뒤에 누른 쪽이 받음 (행 잠금으로 줄을 세움)
    // * 처리를 되돌리는 경로는 없음 — 잘못 처리했으면 사용자가 새로 제보함
    REPORT_ALREADY_RESOLVED(HttpStatus.CONFLICT, "이미 처리한 제보입니다."),

    // 멈춘 이벤트를 관리자가 다시 보냈는데 또 실패함
    //
    // * 보냈다고 알고 넘어가는 것이 이 기능이 막으려던 상황이라 성공으로 응답하지 않음
    // * 이름과 문구는 policy 의 관리자 outbox 와 같음
    OUTBOX_REPUBLISH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이벤트 재발행에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
