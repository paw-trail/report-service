package com.pawtrail.report.application.service;

import com.pawtrail.report.domain.repository.ReportRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 탈퇴한 계정의 제보를 지웁니다. account.withdrawn 을 받은 소비자가 부릅니다.
 *
 * 처리 전 · 처리한 것을 가리지 않고 전부 행째 지웁니다.
 * 탈퇴하면 그 사람의 데이터를 지운다는 약속이 pet · user 에 이미 서 있고, 제보 본문은 그 사람이 쓴 글입니다.
 * 정정의 이유는 정정한 쪽이 들고 있습니다. 조건은 policy 의 정정 이력에 남습니다.
 *
 * 이미 나간 report.resolved 의 outbox 행은 지우지 않습니다.
 * 발행이 끝난 기록이고, 거기에는 사용자가 쓴 본문이 아니라 관리자의 처리 메모가 실려 있습니다.
 * 다른 서비스들도 탈퇴 때 outbox 행은 그대로 둡니다.
 *
 * 이벤트를 따로 내지 않습니다. auth 가 이미 account.withdrawn 을 냈습니다.
 *
 * 트랜잭션을 여기서 열지 않습니다.
 * Inbox 가 "처리한 이벤트" 기록과 이 삭제를 한 트랜잭션으로 묶어야 하므로 그쪽이 엽니다.
 * 둘이 갈라지면 지웠는데 처리 기록이 없어 다시 지우거나, 기록만 남고 안 지워지는 틈이 생깁니다.
 *
 * 지운 것이 없어도 정상입니다. 제보를 한 번도 안 한 사람이 대부분입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountWithdrawnService {

    private final ReportRepository reportRepository;

    public void withdraw(UUID accountId) {
        int deleted = reportRepository.deleteAllByAccountId(accountId);
        log.info("탈퇴한 계정의 제보를 정리했습니다: accountId={}, report={}", accountId, deleted);
    }
}
