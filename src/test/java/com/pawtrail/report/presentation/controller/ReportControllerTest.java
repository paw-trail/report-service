package com.pawtrail.report.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pawtrail.common.enums.Role;
import com.pawtrail.common.response.CommonApiResponse;
import com.pawtrail.common.response.PageResponse;
import com.pawtrail.common.security.principal.CustomUserPrincipal;
import com.pawtrail.report.application.dto.input.ReportCreateInput;
import com.pawtrail.report.application.dto.output.ReportCardOutput;
import com.pawtrail.report.application.dto.output.ReportCreateOutput;
import com.pawtrail.report.application.service.ReportService;
import com.pawtrail.report.domain.enums.ReportType;
import com.pawtrail.report.presentation.request.ReportCreateRequest;
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
 * 요청이 서비스 입력으로 바뀌는 자리와 응답 상태를 검사합니다.
 *
 * 컨트롤러를 직접 부르고 서비스는 흉내 냅니다.
 * 컨트롤러가 조립하는 자리는 직접 부르는 검사를 둡니다 — search 에서 여기서 터진 적이 있습니다.
 */
@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    private static final UUID ACCOUNT = UUID.fromString("01999999-0000-7000-8000-000000000001");
    private static final UUID PLACE = UUID.fromString("01999999-0000-7000-8000-00000000aaaa");
    private static final UUID SAVED = UUID.fromString("01999999-0000-7000-8000-000000000101");

    private final CustomUserPrincipal principal = new CustomUserPrincipal(ACCOUNT, Role.USER);

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController controller;

    @Captor
    private ArgumentCaptor<ReportCreateInput> inputCaptor;

    @Test
    @DisplayName("제출은 201 과 새 식별자를 돌려주고, 빈 칸 이름은 안 온 것으로 넘긴다")
    void 제출() {
        when(reportService.submit(eq(ACCOUNT), inputCaptor.capture())).thenReturn(new ReportCreateOutput(SAVED));

        ResponseEntity<CommonApiResponse<ReportCreateOutput>> response = controller.submit(principal,
                new ReportCreateRequest(PLACE, ReportType.CLOSED, "", null, "문을 닫았어요", null, null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getData().reportId()).isEqualTo(SAVED);
        assertThat(inputCaptor.getValue().fieldName()).isNull();
        assertThat(inputCaptor.getValue().placeId()).isEqualTo(PLACE);
    }

    @Test
    @DisplayName("내 목록은 쪽 번호와 크기를 그대로 넘긴다")
    void 내_목록() {
        PageResponse<ReportCardOutput> empty =
                new PageResponse<>(List.of(), new PageResponse.PageInfo(2, 5, 0, 0));
        when(reportService.getMine(ACCOUNT, 2, 5)).thenReturn(empty);

        ResponseEntity<CommonApiResponse<PageResponse<ReportCardOutput>>> response =
                controller.getMine(principal, 2, 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isSameAs(empty);
        verify(reportService).getMine(ACCOUNT, 2, 5);
    }
}
