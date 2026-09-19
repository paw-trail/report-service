package com.pawtrail.report.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pawtrail.common.exception.CommonErrorCode;
import com.pawtrail.common.exception.CustomException;
import com.pawtrail.common.message.DomainEvent;
import com.pawtrail.common.message.outbox.OutboxEventRecorder;
import com.pawtrail.common.response.PageResponse;
import com.pawtrail.report.application.dto.input.ReportResolveInput;
import com.pawtrail.report.application.dto.output.AdminReportCardOutput;
import com.pawtrail.report.application.dto.output.ReportResolveOutput;
import com.pawtrail.report.domain.enums.ReportStatus;
import com.pawtrail.report.domain.enums.ReportType;
import com.pawtrail.report.domain.event.payload.ReportResolvedEvent;
import com.pawtrail.report.domain.exception.ReportErrorCode;
import com.pawtrail.report.domain.model.Report;
import com.pawtrail.report.domain.provider.PlaceProvider;
import com.pawtrail.report.domain.provider.UserProvider;
import com.pawtrail.report.domain.provider.dto.PlaceData;
import com.pawtrail.report.domain.provider.dto.UserData;
import com.pawtrail.report.domain.repository.ReportRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * 관리자 목록의 조립과 처리의 차례를 검사합니다.
 *
 * 저장소 · place · user · outbox 기록은 흉내 냅니다. 잠금이 실제로 걸리는지는 ReportRepositoryImplTest 가 봅니다.
 */
@ExtendWith(MockitoExtension.class)
class ReportAdminServiceTest {

    private static final UUID REPORT = UUID.fromString("01999999-0000-7000-8000-000000000101");
    private static final UUID ACCOUNT = UUID.fromString("01999999-0000-7000-8000-000000000001");
    private static final UUID OTHER_ACCOUNT = UUID.fromString("01999999-0000-7000-8000-000000000002");
    private static final UUID PLACE = UUID.fromString("01999999-0000-7000-8000-00000000aaaa");
    private static final UUID ADMIN = UUID.fromString("01999999-0000-7000-8000-00000000a0a0");

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private PlaceProvider placeProvider;

    @Mock
    private UserProvider userProvider;

    @Mock
    private OutboxEventRecorder outboxEventRecorder;

    @InjectMocks
    private ReportAdminService reportAdminService;

    @Captor
    private ArgumentCaptor<DomainEvent> eventCaptor;

    @Test
    @DisplayName("목록은 장소 이름과 제보자 닉네임 · 사진을 채우고, 못 받은 사람은 계정만 남긴다")
    void 목록() {
        Report mine = closed(ACCOUNT);
        Report other = closed(OTHER_ACCOUNT);
        when(reportRepository.findForAdmin(ReportStatus.PENDING, 0, 20))
                .thenReturn(new PageImpl<>(List.of(mine, other), PageRequest.of(0, 20), 2));
        when(placeProvider.findByIds(any())).thenReturn(Map.of(PLACE, new PlaceData(PLACE, "노들섬")));
        when(userProvider.findByIds(any()))
                .thenReturn(Map.of(ACCOUNT, new UserData(ACCOUNT, "준기", "https://img/a.png")));

        PageResponse<AdminReportCardOutput> page = reportAdminService.getReports(ReportStatus.PENDING, 0, 20);

        assertThat(page.content()).hasSize(2);
        assertThat(page.content().get(0).placeName()).isEqualTo("노들섬");
        assertThat(page.content().get(0).reporter().nickname()).isEqualTo("준기");
        assertThat(page.content().get(0).reporter().profileImageUrl()).isEqualTo("https://img/a.png");
        assertThat(page.content().get(1).reporter().accountId()).isEqualTo(OTHER_ACCOUNT);
        assertThat(page.content().get(1).reporter().nickname()).isNull();
    }

    @Test
    @DisplayName("place · user 를 못 부르면 그 칸만 비우고 목록은 낸다")
    void 목록_실패() {
        when(reportRepository.findForAdmin(null, 0, 20))
                .thenReturn(new PageImpl<>(List.of(closed(ACCOUNT)), PageRequest.of(0, 20), 1));
        when(placeProvider.findByIds(any())).thenReturn(null);
        when(userProvider.findByIds(any())).thenReturn(null);

        PageResponse<AdminReportCardOutput> page = reportAdminService.getReports(null, 0, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).placeName()).isNull();
        assertThat(page.content().get(0).reporter().accountId()).isEqualTo(ACCOUNT);
        assertThat(page.content().get(0).reporter().nickname()).isNull();
    }

    @Test
    @DisplayName("쪽이 비어 있으면 place 도 user 도 부르지 않는다")
    void 빈_쪽() {
        when(reportRepository.findForAdmin(ReportStatus.ACCEPTED, 5, 20)).thenReturn(Page.empty());

        PageResponse<AdminReportCardOutput> page = reportAdminService.getReports(ReportStatus.ACCEPTED, 5, 20);

        assertThat(page.content()).isEmpty();
        verifyNoInteractions(placeProvider, userProvider);
    }

    @Test
    @DisplayName("처리하면 결과 다섯 칸을 돌려주고 같은 트랜잭션에서 report.resolved 를 기록한다")
    void 처리() throws Exception {
        Report report = closed(ACCOUNT);
        setId(report, REPORT);
        when(reportRepository.findByIdForUpdate(REPORT)).thenReturn(Optional.of(report));

        ReportResolveOutput output = reportAdminService.resolve(
                REPORT, ADMIN, new ReportResolveInput(ReportStatus.ACCEPTED, "폐업을 확인했습니다"));

        assertThat(output.reportId()).isEqualTo(REPORT);
        assertThat(output.status()).isEqualTo(ReportStatus.ACCEPTED);
        assertThat(output.memo()).isEqualTo("폐업을 확인했습니다");
        assertThat(output.reviewedBy()).isEqualTo(ADMIN.toString());
        assertThat(output.reviewedAt()).isNotNull();

        verify(outboxEventRecorder).record(eventCaptor.capture());
        ReportResolvedEvent event = (ReportResolvedEvent) eventCaptor.getValue();
        assertThat(event.reportId()).isEqualTo(REPORT);
        assertThat(event.accountId()).isEqualTo(ACCOUNT);
        assertThat(event.status()).isEqualTo(ReportStatus.ACCEPTED);
        assertThat(event.placeId()).isEqualTo(PLACE);
    }

    @Test
    @DisplayName("처리 결과가 PENDING 이면 400 이고 행을 읽지도 않는다")
    void 처리_결과_400() {
        assertThatThrownBy(() -> reportAdminService.resolve(
                REPORT, ADMIN, new ReportResolveInput(ReportStatus.PENDING, "메모")))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.VALIDATION_FAILED));

        verifyNoInteractions(reportRepository, outboxEventRecorder);
    }

    @Test
    @DisplayName("없는 제보는 404, 이미 처리한 제보는 409 이고 둘 다 이벤트를 남기지 않는다")
    void 없음_이미() {
        Report done = closed(ACCOUNT);
        done.resolve(ReportStatus.REJECTED, "확인되지 않았습니다", ADMIN.toString());
        when(reportRepository.findByIdForUpdate(REPORT)).thenReturn(Optional.empty(), Optional.of(done));

        assertThatThrownBy(() -> reportAdminService.resolve(
                REPORT, ADMIN, new ReportResolveInput(ReportStatus.ACCEPTED, "메모")))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ReportErrorCode.REPORT_NOT_FOUND));
        assertThatThrownBy(() -> reportAdminService.resolve(
                REPORT, ADMIN, new ReportResolveInput(ReportStatus.ACCEPTED, "메모")))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ReportErrorCode.REPORT_ALREADY_RESOLVED));

        verify(outboxEventRecorder, never()).record(any());
        assertThat(done.getStatus()).isEqualTo(ReportStatus.REJECTED);
    }

    private static Report closed(UUID accountId) {
        return Report.submit(accountId, PLACE, ReportType.CLOSED, null, null, "문을 닫았어요", null, null);
    }

    private static void setId(Report report, UUID id) throws Exception {
        Field field = Report.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(report, id);
    }
}
