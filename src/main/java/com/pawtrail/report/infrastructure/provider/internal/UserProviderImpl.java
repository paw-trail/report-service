package com.pawtrail.report.infrastructure.provider.internal;

import com.pawtrail.common.response.CommonApiResponse;
import com.pawtrail.report.domain.provider.UserProvider;
import com.pawtrail.report.domain.provider.dto.UserData;
import com.pawtrail.report.infrastructure.provider.internal.dto.UserResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 도메인이 선언한 약속을 user 서비스 호출로 구현합니다.
 *
 * PlaceProviderImpl 과 같은 모양입니다. 부르는 곳과 받는 칸만 다릅니다.
 */
@Slf4j
@Component
public class UserProviderImpl implements UserProvider {

    private static final String BASE_URL = "lb://user-service";

    /**
     * 한 번에 보낼 수 있는 사용자 수입니다.
     *
     * user 쪽에는 상한이 걸려 있지 않습니다.
     * 그래도 주소 길이가 끝없이 늘지 않게 place 와 같은 100 으로 나눠 부릅니다.
     * 관리자 목록 한 쪽이 20건이라 지금은 한 번이면 끝납니다.
     */
    private static final int BATCH_SIZE = 100;

    private final RestClient restClient;

    /**
     * @Qualifier 를 반드시 붙여야 합니다. 이유는 PlaceProviderImpl 과 같습니다.
     */
    public UserProviderImpl(
            @Qualifier("internalRestClientBuilder") RestClient.Builder builder) {

        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public Map<UUID, UserData> findByIds(Collection<UUID> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return Map.of();
        }

        List<UUID> targets = new ArrayList<>(accountIds);
        Map<UUID, UserData> result = new LinkedHashMap<>();

        for (int from = 0; from < targets.size(); from += BATCH_SIZE) {
            int to = Math.min(from + BATCH_SIZE, targets.size());

            Map<UUID, UserData> chunk = requestChunk(targets.subList(from, to));
            if (chunk == null) {
                return null;
            }
            result.putAll(chunk);
        }
        return result;
    }

    private Map<UUID, UserData> requestChunk(List<UUID> accountIds) {
        try {
            CommonApiResponse<List<UserResponse>> response = restClient.get()
                    .uri(builder -> builder.path("/internal/users")
                            .queryParam("ids", accountIds)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.getData() == null) {
                log.warn("사용자 응답이 비어 있습니다: 요청 {}건", accountIds.size());
                return null;
            }
            return toMap(response.getData());

        } catch (Exception e) {
            log.warn("사용자를 받아오지 못했습니다: 요청 {}건, reason={}", accountIds.size(), e.getMessage());
            return null;
        }
    }

    private Map<UUID, UserData> toMap(List<UserResponse> responses) {
        Map<UUID, UserData> result = new LinkedHashMap<>();
        for (UserResponse response : responses) {
            UUID accountId = parseUuidOrNull(response.accountId());
            if (accountId == null) {
                continue;
            }
            result.put(accountId, new UserData(accountId, response.nickname(), response.profileImageUrl()));
        }
        return result;
    }

    private static UUID parseUuidOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            log.warn("사용자 식별자가 UUID 형식이 아닙니다: {}", value);
            return null;
        }
    }
}
