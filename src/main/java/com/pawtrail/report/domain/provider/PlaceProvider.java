package com.pawtrail.report.domain.provider;

import com.pawtrail.report.domain.provider.dto.PlaceData;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * place 서비스에서 장소를 받아오는 약속입니다.
 *
 * 이 인터페이스에 HTTP 도 RestClient 도 나오지 않습니다.
 * 무엇을 받아오는지만 적고 어떻게 받아오는지는 infrastructure 가 정합니다.
 */
public interface PlaceProvider {

    /**
     * 여러 장소를 한 번에 받아옵니다. 목록 한 쪽의 장소 이름을 채울 때 씁니다.
     *
     * 제보를 받을 때는 부르지 않습니다.
     * 제보는 장소 상세에서만 시작되고, place 가 잠시 멈춰도 제보는 받아야 하기 때문입니다.
     *
     * 없는 식별자는 결과에서 빠집니다. 오류로 보지 않습니다.
     *
     * 호출이 실패하면 null 을 돌려줍니다. 예외를 던지지 않습니다.
     *
     *   빈 Map    물어봤는데 남아 있는 장소가 없었다
     *   null      물어보지 못했다
     *
     * 부르는 쪽은 어느 경우든 이름만 비운 채 목록을 냅니다.
     * 제보 목록의 본체는 유형 · 내용 · 상태 · 메모라 이름 없이도 읽히기 때문입니다.
     * 이름이 없으면 카드가 성립하지 않는 즐겨찾기와 다른 점입니다.
     */
    Map<UUID, PlaceData> findByIds(Collection<UUID> placeIds);
}
