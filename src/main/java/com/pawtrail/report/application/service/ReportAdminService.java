package com.pawtrail.report.application.service;

import com.pawtrail.common.exception.CommonErrorCode;
import com.pawtrail.common.exception.CustomException;
import com.pawtrail.common.message.outbox.OutboxEventRecorder;
import com.pawtrail.common.response.PageResponse;
import com.pawtrail.report.application.dto.input.ReportResolveInput;
import com.pawtrail.report.application.dto.output.AdminReportCardOutput;
import com.pawtrail.report.application.dto.output.AdminReportCardOutput.ReporterOutput;
import com.pawtrail.report.application.dto.output.ReportResolveOutput;
import com.pawtrail.report.domain.enums.ReportStatus;
import com.pawtrail.report.domain.event.payload.ReportResolvedEvent;
import com.pawtrail.report.domain.exception.ReportErrorCode;
import com.pawtrail.report.domain.model.Report;
import com.pawtrail.report.domain.provider.PlaceProvider;
import com.pawtrail.report.domain.provider.UserProvider;
import com.pawtrail.report.domain.provider.dto.PlaceData;
import com.pawtrail.report.domain.provider.dto.UserData;
import com.pawtrail.report.domain.repository.ReportRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자가 제보를 보고 처리하는 일을 맡습니다.
 *
 * 처리해도 장소 · 조건 값은 바뀌지 않습니다.
 * 관리자가 place · policy 의 관리자 API 로 먼저 고치고, 여기서는 결과만 남기고 알립니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportAdminService {

    private final ReportRepository reportRepository;
    private final PlaceProvider placeProvider;
    private final UserProvider userProvider;
    private final OutboxEventRecorder outboxEventRecorder;

    /**
     * 관리자 목록을 최신순으로 한 쪽 돌려줍니다.
     *
     * 상태를 주면 그 상태만, 비워 두면 전부입니다. 처리 전 것만 보려면 PENDING 을 줍니다.
     *
     * 장소 이름은 place 에서, 제보자 닉네임 · 사진은 user 에서 받아 채웁니다.
     * 둘 중 어느 쪽을 못 불러도 목록은 냅니다. 그 칸만 비웁니다.
     * 한 쪽이 비어 있으면 둘 다 부르지 않습니다.
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminReportCardOutput> getReports(ReportStatus status, int page, int size) {
        Page<Report> reports = reportRepository.findForAdmin(status, page, size);
        List<Report> content = reports.getContent();
        if (content.isEmpty()) {
            return PageResponse.from(reports, report -> toCard(report, Map.of(), Map.of()));
        }

        Map<UUID, PlaceData> places = orEmpty(
                placeProvider.findByIds(content.stream().map(Report::getPlaceId).distinct().toList()),
                "장소 이름");
        Map<UUID, UserData> users = orEmpty(
                userProvider.findByIds(content.stream().map(Report::getAccountId).distinct().toList()),
                "제보자");

        return PageResponse.from(reports, report -> toCard(report, places, users));
    }

    /**
     * 제보를 처리합니다. 승인이나 반려 한 번뿐입니다.
     *
     * 차례가 넷입니다.
     *   ① 처리 결과가 승인 · 반려인지 — 아니면 400 VALIDATION_FAILED
     *   ② 행을 잠가 읽음 — 없으면 404 REPORT_NOT_FOUND
     *   ③ 처리 전인지 — 아니면 409 REPORT_ALREADY_RESOLVED
     *   ④ 처리하고 같은 트랜잭션에서 report.resolved 를 outbox 에 기록
     *
     * 잠가 읽는 이유는 두 관리자가 같은 카드를 동시에 누를 때 결과가 두 번 나가지 않게 하려는 것입니다.
     * 뒤에 온 쪽은 앞의 처리가 커밋될 때까지 기다렸다가 처리된 상태를 보고 409 를 받습니다.
     *
     * 이벤트를 outbox 에 기록하므로 처리와 알림이 함께 되거나 함께 안 됩니다.
     * 커밋 직후 공통 모듈이 곧바로 보내고, 실패하면 되풀이 발행이 이어받습니다.
     */
    @Transactional
    public ReportResolveOutput resolve(UUID reportId, UUID adminId, ReportResolveInput input) {
        if (input.status() != ReportStatus.ACCEPTED && input.status() != ReportStatus.REJECTED) {
            log.info("처리 결과는 승인 · 반려만 됩니다: reportId={}, status={}", reportId, input.status());
            throw new CustomException(CommonErrorCode.VALIDATION_FAILED);
        }

        Report report = reportRepository.findByIdForUpdate(reportId)
                .orElseThrow(() -> new CustomException(ReportErrorCode.REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING) {
            log.info("이미 처리한 제보입니다: reportId={}, status={}", reportId, report.getStatus());
            throw new CustomException(ReportErrorCode.REPORT_ALREADY_RESOLVED);
        }

        report.resolve(input.status(), input.memo(), adminId.toString());
        outboxEventRecorder.record(ReportResolvedEvent.from(report));

        log.info("제보를 처리했습니다: reportId={}, status={}, admin={}", reportId, report.getStatus(), adminId);
        return ReportResolveOutput.from(report);
    }

    private static AdminReportCardOutput toCard(Report report,
                                                Map<UUID, PlaceData> places,
                                                Map<UUID, UserData> users) {
        PlaceData place = places.get(report.getPlaceId());
        UserData user = users.get(report.getAccountId());
        ReporterOutput reporter = new ReporterOutput(
                report.getAccountId(),
                user == null ? null : user.nickname(),
                user == null ? null : user.profileImageUrl());
        return AdminReportCardOutput.of(report, place == null ? null : place.name(), reporter);
    }

    private static <K, V> Map<K, V> orEmpty(Map<K, V> found, String what) {
        if (found == null) {
            log.warn("{} 을(를) 받지 못해 그 칸을 비운 채 목록을 냅니다", what);
            return Map.of();
        }
        return found;
    }
}
