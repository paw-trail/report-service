package com.pawtrail.report.application.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pawtrail.report.domain.repository.ReportRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 탈퇴를 받으면 그 계정의 제보를 지우는지 검사합니다.
 *
 * 실제로 행이 지워지는지는 ReportRepositoryImplTest 가 봅니다.
 */
@ExtendWith(MockitoExtension.class)
class AccountWithdrawnServiceTest {

    private static final UUID ACCOUNT = UUID.fromString("01999999-0000-7000-8000-000000000001");

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private AccountWithdrawnService accountWithdrawnService;

    @Test
    @DisplayName("그 계정의 제보를 지운다 — 지운 것이 없어도 정상이다")
    void 지운다() {
        when(reportRepository.deleteAllByAccountId(ACCOUNT)).thenReturn(0);

        accountWithdrawnService.withdraw(ACCOUNT);

        verify(reportRepository).deleteAllByAccountId(ACCOUNT);
    }
}
