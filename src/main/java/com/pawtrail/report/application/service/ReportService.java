package com.pawtrail.report.application.service;

import com.pawtrail.common.exception.CommonErrorCode;
import com.pawtrail.common.exception.CustomException;
import com.pawtrail.common.response.PageResponse;
import com.pawtrail.report.application.dto.input.ReportCreateInput;
import com.pawtrail.report.application.dto.output.ReportCardOutput;
import com.pawtrail.report.application.dto.output.ReportCreateOutput;
import com.pawtrail.report.domain.exception.ReportErrorCode;
import com.pawtrail.report.domain.model.Report;
import com.pawtrail.report.domain.provider.PlaceProvider;
import com.pawtrail.report.domain.provider.dto.PlaceData;
import com.pawtrail.report.domain.repository.ReportRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자가 제보를 올리고 자기 제보를 보는 일을 맡습니다.
 *
 * 관리자가 처리하는 일은 여기 두지 않습니다.
 * 부르는 사람이 다르고 권한이 다르며, 처리에는 행 잠금과 이벤트 발행이 붙습니다.
 */
@Slf4j
@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final PlaceProvider placeProvider;
    private final int dailyLimit;

    /**
     * 하루 상한은 config 의 app.report.daily-limit 에서 받습니다.
     *
     * 코드에 같은 기본값(20)을 둡니다.
     * 설정 서버 없이 떠도 상한이 풀리지 않고, 테스트 설정에 사본을 둘 필요도 없습니다.
     * search 가 재색인 시각과 잠금 만료를 이렇게 둔 것과 같은 방식입니다.
     */
    public ReportService(ReportRepository reportRepository,
                         PlaceProvider placeProvider,
                         @Value("${app.report.daily-limit:20}") int dailyLimit) {
        this.reportRepository = reportRepository;
        this.placeProvider = placeProvider;
        this.dailyLimit = dailyLimit;
    }

    /**
     * 제보를 받습니다.
     *
     * 차례가 셋입니다.
     *   ① 유형별 칸 규칙 — 어기면 400 VALIDATION_FAILED
     *   ② 처리 중인 같은 제보 — 있으면 409 REPORT_ALREADY_PENDING
     *   ③ 하루 상한 — 넘기면 429 REPORT_DAILY_LIMIT
     *
     * ②를 ③보다 먼저 봅니다.
     * 둘 다 걸리는 요청이라면 "이미 처리 중인 같은 제보가 있다" 가 사용자에게 더 쓸모 있는 말입니다.
     *
     * 다른 서비스를 부르지 않습니다.
     * 장소가 실재하는지 묻지 않는 이유는 제보가 장소 상세에서만 시작되고,
     * place 가 잠시 멈췄다고 "정보가 틀렸어요" 를 못 받으면 안 되기 때문입니다.
     * 후기가 실재하는지도 묻지 않습니다. review 에는 후기를 식별자로 찾는 조회가 아직 없습니다.
     *
     * 하루 상한은 서울 날짜 00:00 부터 저장된 제보를 셉니다.
     * 두 요청이 동시에 들어오면 상한을 한두 건 넘길 수 있으나 도배를 막는 장치라 받아들입니다.
     */
    @Transactional
    public ReportCreateOutput submit(UUID accountId, ReportCreateInput input) {
        List<String> violations = input.reportType().violations(
                input.fieldName(), input.reportedValue(), input.visitedAt(), input.targetReviewId());
        if (!violations.isEmpty()) {
            log.info("유형별 칸 규칙에 맞지 않는 제보입니다: accountId={}, type={}, violations={}",
                    accountId, input.reportType(), violations);
            throw new CustomException(CommonErrorCode.VALIDATION_FAILED);
        }

        boolean duplicated = reportRepository.existsPending(
                accountId, input.placeId(), input.reportType(), input.fieldName(), input.targetReviewId());
        if (duplicated) {
            log.info("처리 중인 같은 제보가 있습니다: accountId={}, placeId={}, type={}, field={}",
                    accountId, input.placeId(), input.reportType(), input.fieldName());
            throw new CustomException(ReportErrorCode.REPORT_ALREADY_PENDING);
        }

        long today = reportRepository.countSubmittedSince(accountId, LocalDate.now().atStartOfDay());
        if (today >= dailyLimit) {
            log.info("오늘 올릴 수 있는 제보를 다 썼습니다: accountId={}, today={}, limit={}",
                    accountId, today, dailyLimit);
            throw new CustomException(ReportErrorCode.REPORT_DAILY_LIMIT);
        }

        Report saved = reportRepository.saveNew(Report.submit(
                accountId,
                input.placeId(),
                input.reportType(),
                input.fieldName(),
                input.reportedValue(),
                input.content(),
                input.visitedAt(),
                input.targetReviewId()));

        log.info("제보를 받았습니다: reportId={}, accountId={}, placeId={}, type={}",
                saved.getId(), accountId, saved.getPlaceId(), saved.getReportType());
        return new ReportCreateOutput(saved.getId());
    }

    /**
     * 내 제보를 최신순으로 한 쪽 돌려줍니다. 마이페이지 고객센터가 부릅니다.
     *
     * 장소 이름은 place 에서 받아 채웁니다.
     * 못 받으면 이름만 비우고 목록은 그대로 냅니다. 목록의 본체는 유형 · 내용 · 상태 · 메모입니다.
     *
     * 한 쪽이 비어 있으면 place 를 부르지 않습니다. 물어볼 것이 없습니다.
     */
    @Transactional(readOnly = true)
    public PageResponse<ReportCardOutput> getMine(UUID accountId, int page, int size) {
        Page<Report> reports = reportRepository.findByAccountId(accountId, page, size);
        Map<UUID, PlaceData> places = findPlaces(reports.getContent());
        return PageResponse.from(reports, report ->
                ReportCardOutput.of(report, nameOf(places, report.getPlaceId())));
    }

    private Map<UUID, PlaceData> findPlaces(List<Report> reports) {
        if (reports.isEmpty()) {
            return Map.of();
        }
        List<UUID> placeIds = reports.stream().map(Report::getPlaceId).distinct().toList();
        Map<UUID, PlaceData> places = placeProvider.findByIds(placeIds);
        if (places == null) {
            log.warn("장소 이름을 받지 못해 이름 없이 목록을 냅니다: 장소 {}곳", placeIds.size());
            return Map.of();
        }
        return places;
    }

    private static String nameOf(Map<UUID, PlaceData> places, UUID placeId) {
        PlaceData place = places.get(placeId);
        return place == null ? null : place.name();
    }
}
