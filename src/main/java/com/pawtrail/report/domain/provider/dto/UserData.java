package com.pawtrail.report.domain.provider.dto;

import java.util.UUID;

/**
 * user 에서 받아온 사용자 한 명입니다.
 *
 * @param accountId       계정 식별자입니다.
 * @param nickname        닉네임입니다.
 * @param profileImageUrl 프로필 사진 주소입니다. 없을 수 있습니다.
 */
public record UserData(UUID accountId, String nickname, String profileImageUrl) {
}
