package com.pawtrail.report.domain.provider.dto;

import java.util.UUID;

/**
 * place 에서 받아온 장소 하나입니다.
 *
 * 제보 목록은 장소 이름만 보여주므로 이름만 담습니다.
 * 사진이나 좌표가 필요해지면 그때 칸을 더하고, place 응답은 이미 그 값을 싣고 있습니다.
 *
 * @param placeId 장소 식별자입니다.
 * @param name    장소 이름입니다.
 */
public record PlaceData(UUID placeId, String name) {
}
