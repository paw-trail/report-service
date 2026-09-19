package com.pawtrail.report.presentation.controller;

import com.pawtrail.common.response.CommonApiResponse;
import com.pawtrail.common.response.PageResponse;
import com.pawtrail.common.security.annotation.CurrentUser;
import com.pawtrail.common.security.principal.CustomUserPrincipal;
import com.pawtrail.report.application.dto.output.AdminReportCardOutput;
import com.pawtrail.report.application.dto.output.OutboxMessageOutput;
import com.pawtrail.report.application.dto.output.ReportResolveOutput;
import com.pawtrail.report.application.service.AdminOutboxService;
import com.pawtrail.report.application.service.ReportAdminService;
import com.pawtrail.report.domain.enums.ReportStatus;
import com.pawtrail.report.presentation.request.ReportResolveRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자가 제보를 보고 처리하는 API 입니다.
 *
 * 공통 보안 체인이 /api/v1/admin/** 를 ADMIN 역할로 막습니다. 여기서 역할을 다시 보지 않습니다.
 * 게이트웨이의 /api/v1/admin/reports/** 한 줄이 네 경로를 다 덮습니다.
 */
@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportAdminService reportAdminService;
    private final AdminOutboxService adminOutboxService;

    /**
     * 제보 목록을 최신순으로 한 쪽 돌려줍니다.
     *
     * status 를 주면 그 상태만, 비워 두면 전부입니다. 모르는 값이면 400 입니다.
     * 쪽은 내 제보 목록과 같이 page (0부터) · size (1~100, 기본 20) 로 받습니다.
     */
    @GetMapping
    public ResponseEntity<CommonApiResponse<PageResponse<AdminReportCardOutput>>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") @PositiveOrZero(message = "page 는 0 이상이어야 합니다") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size 는 1 이상이어야 합니다")
            @Max(value = 100, message = "size 는 100 이하여야 합니다") int size) {

        PageResponse<AdminReportCardOutput> response = reportAdminService.getReports(status, page, size);
        return ResponseEntity.ok(CommonApiResponse.success(response));
    }

    /**
     * 제보를 승인하거나 반려합니다. 제보 하나에 한 번뿐입니다.
     *
     * 처리로 바뀐 칸만 돌려줍니다. 장소 이름과 제보자는 목록에 이미 있습니다.
     * <pre>
     * 400  VALIDATION_FAILED        처리 결과가 PENDING · 메모가 비었거나 500자를 넘음
     * 404  REPORT_NOT_FOUND         그런 제보가 없음 (탈퇴로 지워졌을 수 있음)
     * 409  REPORT_ALREADY_RESOLVED  이미 처리함
     * </pre>
     */
    @PatchMapping("/{reportId}")
    public ResponseEntity<CommonApiResponse<ReportResolveOutput>> resolve(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable UUID reportId,
            @Valid @RequestBody ReportResolveRequest request) {

        ReportResolveOutput response =
                reportAdminService.resolve(reportId, principal.accountId(), request.toInput());
        return ResponseEntity.ok(CommonApiResponse.success(response));
    }

    /**
     * 발행이 끝내 실패해 멈춰 있는 이벤트를 봅니다. 아직 재시도 중인 건은 나오지 않습니다.
     *
     * 경로 깊이가 PATCH /{reportId} 와 같으나 메서드가 달라 겹치지 않습니다.
     */
    @GetMapping("/outbox")
    public ResponseEntity<CommonApiResponse<PageResponse<OutboxMessageOutput>>> findGivenUpOutbox(
            @PageableDefault(size = 20) Pageable pageable) {

        PageResponse<OutboxMessageOutput> response = adminOutboxService.findGivenUp(pageable);
        return ResponseEntity.ok(CommonApiResponse.success(response));
    }

    /**
     * 한 건을 다시 발행합니다. 위 목록의 id 를 그대로 넘깁니다.
     * <pre>
     * 500  OUTBOX_REPUBLISH_FAILED  발행에 실패함 · 재시도 횟수가 오르고 마지막 오류가 갱신됨
     * </pre>
     */
    @PostMapping("/outbox/{outboxId}/retry")
    public ResponseEntity<CommonApiResponse<Void>> republishOutbox(@PathVariable UUID outboxId) {
        adminOutboxService.republish(outboxId);
        return ResponseEntity.ok(CommonApiResponse.success(null));
    }
}
