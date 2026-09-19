package com.pawtrail.report.domain.provider;

import com.pawtrail.report.domain.provider.dto.UserData;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * user 서비스에서 사용자의 닉네임 · 사진을 받아오는 약속입니다.
 *
 * 관리자 목록에서 제보자를 보여줄 때만 씁니다. 제보를 받거나 처리할 때는 부르지 않습니다.
 *
 * 없는 식별자와 탈퇴한 사람은 결과에서 빠집니다. 오류로 보지 않습니다.
 *
 * 호출이 실패하면 null 을 돌려줍니다. 예외를 던지지 않습니다.
 *
 *   빈 Map    물어봤는데 남아 있는 사람이 없었다
 *   null      물어보지 못했다
 *
 * 부르는 쪽은 어느 경우든 닉네임 · 사진만 비운 채 목록을 냅니다.
 */
public interface UserProvider {

    Map<UUID, UserData> findByIds(Collection<UUID> accountIds);
}
