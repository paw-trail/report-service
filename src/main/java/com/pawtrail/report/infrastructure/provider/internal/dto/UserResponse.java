package com.pawtrail.report.infrastructure.provider.internal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * GET /internal/users?ids= 의 data 안에 담기는 원소입니다.
 *
 * user 의 UserSummaryOutput 과 같은 세 칸입니다. 모르는 칸은 무시합니다.
 * accountId 를 문자열로 받는 것은 그쪽이 문자열로 보내기 때문입니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserResponse(String accountId, String nickname, String profileImageUrl) {
}
