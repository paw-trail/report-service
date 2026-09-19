package com.pawtrail.report.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pawtrail.common.exception.CommonErrorCode;
import com.pawtrail.common.exception.CustomException;
import com.pawtrail.common.response.PageResponse;
import com.pawtrail.report.application.dto.input.ReportCreateInput;
import com.pawtrail.report.application.dto.output.ReportCardOutput;
import com.pawtrail.report.application.dto.output.ReportCreateOutput;
import com.pawtrail.report.domain.enums.ReportStatus;
import com.pawtrail.report.domain.enums.ReportType;
import com.pawtrail.report.domain.exception.ReportErrorCode;
import com.pawtrail.report.domain.model.Report;
import com.pawtrail.report.domain.provider.PlaceProvider;
import com.pawtrail.report.domain.provider.dto.PlaceData;
import com.pawtrail.report.domain.repository.ReportRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * 제보를 받는 차례와 내 목록의 조립을 검사합니다.
 *
 * 저장소와 place 는 흉내 냅니다. 인덱스 · CHECK 가 실제로 막는지는 ReportRepositoryImplTest 가 봅니다.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final UUID ACCOUNT = UUID.fromString("01999999-0000-7000-8000-000000000001");
    private static final UUID PLACE = UUID.fromString("01999999-0000-7000-8000-00000000aaaa");
    private static final UUID OTHER_PLACE = UUID.fromString("01999999-0000-7000-8000-00000000cccc");
    private static final UUID SAVED = UUID.fromString("01999999-0000-7000-8000-000000000101");

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private PlaceProvider placeProvider;

    @Captor
    private ArgumentCaptor<Report> reportCaptor;

    @Captor
    private ArgumentCaptor<LocalDateTime> sinceCaptor;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(reportRepository, placeProvider, 20);
    }

    @Test
    @DisplayName("규칙에 맞으면 처리 전 상태로 저장하고 새 식별자를 돌려준다")
    void 받는다() {
        Report stored = mock(Report.class);
        when(stored.getId()).thenReturn(SAVED);
        when(reportRepository.existsPending(ACCOUNT, PLACE, ReportType.INFO_WRONG, "tel", null)).thenReturn(false);
        when(reportRepository.countSubmittedSince(eq(ACCOUNT), any(LocalDateTime.class))).thenReturn(0L);
        when(reportRepository.saveNew(any(Report.class))).thenReturn(stored);

        ReportCreateOutput output = reportService.submit(ACCOUNT, new ReportCreateInput(
                PLACE, ReportType.INFO_WRONG, "tel", "02-1234-5678", "번호가 달라요", null, null));

        assertThat(output.reportId()).isEqualTo(SAVED);
        verify(reportRepository).saveNew(reportCaptor.capture());
        Report report = reportCaptor.getValue();
        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(report.getAccountId()).isEqualTo(ACCOUNT);
        assertThat(report.getPlaceId()).isEqualTo(PLACE);
        assertThat(report.getFieldName()).isEqualTo("tel");
        assertThat(report.getReportedValue()).isEqualTo("02-1234-5678");
    }

    @Test
    @DisplayName("유형별 칸 규칙을 어기면 400 이고 저장소를 건드리지 않는다")
    void 칸_규칙() {
        assertThatThrownBy(() -> reportService.submit(ACCOUNT, closed("tel")))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.VALIDATION_FAILED));

        verifyNoInteractions(reportRepository);
    }

    @Test
    @DisplayName("처리 중인 같은 제보가 있으면 409 이고 상한을 세지도 저장하지도 않는다")
    void 중복() {
        when(reportRepository.existsPending(ACCOUNT, PLACE, ReportType.CLOSED, null, null)).thenReturn(true);

        assertThatThrownBy(() -> reportService.submit(ACCOUNT, closed(null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ReportErrorCode.REPORT_ALREADY_PENDING));

        verify(reportRepository, never()).countSubmittedSince(any(), any());
        verify(reportRepository, never()).saveNew(any());
    }

    @Test
    @DisplayName("오늘 00:00 부터 20건을 올렸으면 429 이고, 19건이면 받는다")
    void 하루_상한() {
        when(reportRepository.existsPending(any(), any(), any(), any(), any())).thenReturn(false);
        when(reportRepository.countSubmittedSince(eq(ACCOUNT), sinceCaptor.capture())).thenReturn(20L, 19L);
        when(reportRepository.saveNew(any(Report.class))).thenReturn(mock(Report.class));

        assertThatThrownBy(() -> reportService.submit(ACCOUNT, closed(null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ReportErrorCode.REPORT_DAILY_LIMIT));

        reportService.submit(ACCOUNT, closed(null));

        verify(reportRepository, times(1)).saveNew(any(Report.class));
        assertThat(sinceCaptor.getValue()).isEqualTo(LocalDate.now().atStartOfDay());
    }

    @Test
    @DisplayName("내 목록은 장소 이름을 place 에서 받아 채우고, 없는 장소는 이름만 비운다")
    void 내_목록() {
        Report a = Report.submit(ACCOUNT, PLACE, ReportType.CLOSED, null, null, "문을 닫았어요", null, null);
        Report b = Report.submit(ACCOUNT, OTHER_PLACE, ReportType.INFO_WRONG, "tel", null, "번호가 달라요", null, null);
        when(reportRepository.findByAccountId(ACCOUNT, 0, 20))
                .thenReturn(new PageImpl<>(List.of(a, b), PageRequest.of(0, 20), 2));
        when(placeProvider.findByIds(any())).thenReturn(Map.of(PLACE, new PlaceData(PLACE, "노들섬")));

        PageResponse<ReportCardOutput> page = reportService.getMine(ACCOUNT, 0, 20);

        assertThat(page.content()).hasSize(2);
        assertThat(page.content().get(0).placeName()).isEqualTo("노들섬");
        assertThat(page.content().get(1).placeName()).isNull();
        assertThat(page.content().get(1).fieldName()).isEqualTo("tel");
        assertThat(page.page().totalElements()).isEqualTo(2L);
    }

    @Test
    @DisplayName("place 를 못 부르면 이름만 비우고 목록은 그대로 낸다")
    void place_실패() {
        Report a = Report.submit(ACCOUNT, PLACE, ReportType.CLOSED, null, null, "문을 닫았어요", null, null);
        when(reportRepository.findByAccountId(ACCOUNT, 0, 20))
                .thenReturn(new PageImpl<>(List.of(a), PageRequest.of(0, 20), 1));
        when(placeProvider.findByIds(any())).thenReturn(null);

        PageResponse<ReportCardOutput> page = reportService.getMine(ACCOUNT, 0, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).placeName()).isNull();
        assertThat(page.content().get(0).content()).isEqualTo("문을 닫았어요");
    }

    @Test
    @DisplayName("쪽이 비어 있으면 place 를 부르지 않는다")
    void 빈_쪽() {
        when(reportRepository.findByAccountId(ACCOUNT, 3, 20)).thenReturn(Page.empty());

        PageResponse<ReportCardOutput> page = reportService.getMine(ACCOUNT, 3, 20);

        assertThat(page.content()).isEmpty();
        verifyNoInteractions(placeProvider);
    }

    private static ReportCreateInput closed(String fieldName) {
        return new ReportCreateInput(PLACE, ReportType.CLOSED, fieldName, null, "문을 닫았어요", null, null);
    }
}
