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
    REPORT_DAILY_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "오늘 올릴 수 있는 제보를 다 썼습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
