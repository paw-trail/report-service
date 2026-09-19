package com.pawtrail.report.infrastructure.provider.internal;

import com.pawtrail.common.response.CommonApiResponse;
import com.pawtrail.report.domain.provider.PlaceProvider;
import com.pawtrail.report.domain.provider.dto.PlaceData;
import com.pawtrail.report.infrastructure.provider.internal.dto.PlaceResponse;
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
 * 도메인이 선언한 약속을 place 서비스 호출로 구현합니다.
 *
 * user 의 PlaceProviderImpl 과 같은 모양입니다.
 * 다른 점은 이름 하나만 꺼낸다는 것뿐입니다.
 */
@Slf4j
@Component
public class PlaceProviderImpl implements PlaceProvider {

    private static final String BASE_URL = "lb://place-service";

    /**
     * 한 번에 보낼 수 있는 장소 수입니다.
     *
     * place 가 GET /internal/places?ids= 에 @Size(max = 100) 을 걸어 두었습니다.
     * 넘기면 400 이 납니다. 우리가 고를 수 있는 값이 아니라 place 가 정한 계약이라 설정으로 빼지 않습니다.
     *
     * 목록 한 쪽이 20건이라 지금은 한 번이면 끝나지만,
     * 쪽 크기를 늘리면 바로 넘치므로 나눠 부르는 것을 처음부터 둡니다.
     */
    private static final int BATCH_SIZE = 100;

    private final RestClient restClient;

    /**
     * 빌더를 주입받아 RestClient 를 만듭니다.
     *
     * @Qualifier 를 반드시 붙여야 합니다.
     * 같은 타입의 빈이 셋이고 그중 하나가 @Primary 입니다.
     * 빠뜨리면 아무것도 얹히지 않은 그 빌더가 조용히 주입되어
     * lb:// 를 풀지 못하고 기동이 아니라 호출하는 순간에 실패합니다.
     * 롬복 생성자에는 @Qualifier 가 붙지 않아 생성자를 손으로 씁니다.
     */
    public PlaceProviderImpl(
            @Qualifier("internalRestClientBuilder") RestClient.Builder builder) {

        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    /**
     * 여러 장소를 받아옵니다.
     *
     * 어느 한 묶음이 실패하면 나머지를 포기하고 null 을 돌려줍니다.
     * 일부만 채우면 못 불러온 것과 없어진 것이 똑같이 보이기 때문입니다.
     */
    @Override
    public Map<UUID, PlaceData> findByIds(Collection<UUID> placeIds) {
        if (placeIds == null || placeIds.isEmpty()) {
            return Map.of();
        }

        List<UUID> targets = new ArrayList<>(placeIds);
        Map<UUID, PlaceData> result = new LinkedHashMap<>();

        for (int from = 0; from < targets.size(); from += BATCH_SIZE) {
            int to = Math.min(from + BATCH_SIZE, targets.size());

            Map<UUID, PlaceData> chunk = requestChunk(targets.subList(from, to));
            if (chunk == null) {
                return null;
            }
            result.putAll(chunk);
        }
        return result;
    }

    /**
     * 한 묶음을 요청합니다. 실패하면 null 을 돌려줍니다.
     *
     * 잡는 범위를 Exception 으로 둡니다.
     * 연결 거부 · 시간 초과 · 유레카가 서비스를 못 찾는 것 · 응답 형태가 다른 것까지
     * 결과가 모두 같기 때문입니다. 값을 못 받았다는 것 하나이고 부르는 쪽이 할 일도 하나입니다.
     */
    private Map<UUID, PlaceData> requestChunk(List<UUID> placeIds) {
        try {
            CommonApiResponse<List<PlaceResponse>> response = restClient.get()
                    .uri(builder -> builder.path("/internal/places")
                            .queryParam("ids", placeIds)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.getData() == null) {
                log.warn("장소 응답이 비어 있습니다: 요청 {}건", placeIds.size());
                return null;
            }
            return toMap(response.getData());

        } catch (Exception e) {
            log.warn("장소를 받아오지 못했습니다: 요청 {}건, reason={}", placeIds.size(), e.getMessage());
            return null;
        }
    }

    /**
     * 응답을 도메인 타입으로 바꿔 placeId 로 찾을 수 있게 담습니다.
     *
     * 식별자가 없거나 형식이 어긋난 원소는 건너뜁니다.
     * 부르는 쪽이 자기 목록과 맞출 수 없어 쓸 데가 없고, 하나 때문에 전체를 실패시킬 이유도 없습니다.
     */
    private Map<UUID, PlaceData> toMap(List<PlaceResponse> responses) {
        Map<UUID, PlaceData> result = new LinkedHashMap<>();
        for (PlaceResponse response : responses) {
            UUID placeId = parseUuidOrNull(response.placeId());
            if (placeId == null) {
                continue;
            }
            result.put(placeId, new PlaceData(placeId, response.name()));
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
            log.warn("장소 식별자가 UUID 형식이 아닙니다: {}", value);
            return null;
        }
    }
}
