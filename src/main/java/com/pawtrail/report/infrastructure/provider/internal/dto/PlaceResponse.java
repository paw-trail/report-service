package com.pawtrail.report.infrastructure.provider.internal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * GET /internal/places?ids= 의 data 안에 담기는 원소입니다.
 *
 * 봉투(CommonApiResponse)를 벗긴 안쪽만 담습니다. 벗기는 일은 PlaceProviderImpl 이 합니다.
 *
 * place 는 일곱 칸(placeId · name · placeType · imageUrl · lat · lon · supplyPoint)을 보내지만
 * 제보 목록은 이름만 쓰므로 두 칸만 받습니다. 나머지는 무시합니다.
 * place 가 칸을 더하거나 빼도 이 두 칸이 남아 있는 한 우리 쪽이 깨지지 않습니다.
 *
 * placeId 를 문자열로 받는 것은 그쪽이 문자열로 보내기 때문입니다. UUID 로 바꾸는 일은 구현이 합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlaceResponse(String placeId, String name) {
}
