package com.pawtrail.report.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pawtrail.common.enums.Role;
import com.pawtrail.common.response.CommonApiResponse;
import com.pawtrail.common.response.PageResponse;
import com.pawtrail.common.security.principal.CustomUserPrincipal;
import com.pawtrail.report.application.dto.input.ReportResolveInput;
import com.pawtrail.report.application.dto.output.AdminReportCardOutput;
import com.pawtrail.report.application.dto.output.ReportResolveOutput;
import com.pawtrail.report.application.service.AdminOutboxService;
import com.pawtrail.report.application.service.ReportAdminService;
import com.pawtrail.report.domain.enums.ReportStatus;
import com.pawtrail.report.presentation.request.ReportResolveRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 관리자 요청이 서비스 입력으로 바뀌는 자리와 응답 상태를 검사합니다.
 */
@ExtendWith(MockitoExtension.class)
class AdminReportControllerTest {

    private static final UUID ADMIN = UUID.fromString("01999999-0000-7000-8000-00000000a0a0");
    private static final UUID REPORT = UUID.fromString("01999999-0000-7000-8000-000000000101");

    private final CustomUserPrincipal admin = new CustomUserPrincipal(ADMIN, Role.ADMIN);

    @Mock
    private ReportAdminService reportAdminService;

    @Mock
    private AdminOutboxService adminOutboxService;

    @InjectMocks
    private AdminReportController controller;

    @Captor
    private ArgumentCaptor<ReportResolveInput> inputCaptor;

    @Test
    @DisplayName("처리는 관리자 계정을 처리자로 넘기고 200 과 처리 결과를 돌려준다")
    void 처리() {
        ReportResolveOutput output = new ReportResolveOutput(
                REPORT, ReportStatus.REJECTED, "확인되지 않았습니다", ADMIN.toString(), LocalDateTime.now());
        when(reportAdminService.resolve(eq(REPORT), eq(ADMIN), inputCaptor.capture())).thenReturn(output);

        ResponseEntity<CommonApiResponse<ReportResolveOutput>> response = controller.resolve(
                admin, REPORT, new ReportResolveRequest(ReportStatus.REJECTED, "확인되지 않았습니다"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isSameAs(output);
        assertThat(inputCaptor.getValue().status()).isEqualTo(ReportStatus.REJECTED);
        assertThat(inputCaptor.getValue().memo()).isEqualTo("확인되지 않았습니다");
    }

    @Test
    @DisplayName("목록은 상태 · 쪽 번호 · 크기를 그대로 넘긴다")
    void 목록() {
        PageResponse<AdminReportCardOutput> empty =
                new PageResponse<>(List.of(), new PageResponse.PageInfo(1, 10, 0, 0));
        when(reportAdminService.getReports(ReportStatus.PENDING, 1, 10)).thenReturn(empty);

        ResponseEntity<CommonApiResponse<PageResponse<AdminReportCardOutput>>> response =
                controller.getReports(ReportStatus.PENDING, 1, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isSameAs(empty);
        verify(reportAdminService).getReports(ReportStatus.PENDING, 1, 10);
    }
}
