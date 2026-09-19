package com.pawtrail.report.presentation.controller;

import com.pawtrail.common.response.CommonApiResponse;
import com.pawtrail.common.response.PageResponse;
import com.pawtrail.common.security.annotation.CurrentUser;
import com.pawtrail.common.security.principal.CustomUserPrincipal;
import com.pawtrail.report.application.dto.output.ReportCardOutput;
import com.pawtrail.report.application.dto.output.ReportCreateOutput;
import com.pawtrail.report.application.service.ReportService;
import com.pawtrail.report.presentation.request.ReportCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자가 제보를 올리고 자기 제보를 보는 API 입니다.
 *
 * 게이트웨이의 /api/v1/reports/** 한 줄이 두 경로를 다 덮습니다.
 * 로그인해야 부를 수 있습니다. 공통 보안 체인이 /api/v1/** 를 인증 필수로 둡니다.
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 제보를 올립니다.
     *
     * 장소 상세의 「정보가 틀렸어요」 와 후기 카드의 「신고」 가 부릅니다.
     * 후기 신고도 그 후기가 달린 장소의 placeId 를 함께 보냅니다.
     *
     * 201 과 새 제보의 식별자만 돌려줍니다.
     * 성공하면 언제나 새로 만들어지므로 201 이 사실과 맞습니다.
     * 처리 중인 같은 제보가 있으면 새로 만들지 않고 409 로 거절합니다.
     */
    @PostMapping
    public ResponseEntity<CommonApiResponse<ReportCreateOutput>> submit(
            @CurrentUser CustomUserPrincipal principal,
            @Valid @RequestBody ReportCreateRequest request) {

        ReportCreateOutput response = reportService.submit(principal.accountId(), request.toInput());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonApiResponse.success(response));
    }

    /**
     * 내 제보를 최신순으로 한 쪽 돌려줍니다. 마이페이지 고객센터가 부릅니다.
     *
     * 쪽은 page (0부터) · size (1~100, 기본 20) 로 받습니다.
     * 정렬은 받지 않습니다. 늘 최신순이고, 받으면 차례가 흔들릴 자리가 생깁니다.
     * 범위를 벗어나면 400 입니다.
     */
    @GetMapping("/me")
    public ResponseEntity<CommonApiResponse<PageResponse<ReportCardOutput>>> getMine(
            @CurrentUser CustomUserPrincipal principal,
            @RequestParam(defaultValue = "0") @PositiveOrZero(message = "page 는 0 이상이어야 합니다") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size 는 1 이상이어야 합니다")
            @Max(value = 100, message = "size 는 100 이하여야 합니다") int size) {

        PageResponse<ReportCardOutput> response = reportService.getMine(principal.accountId(), page, size);
        return ResponseEntity.ok(CommonApiResponse.success(response));
    }
}
