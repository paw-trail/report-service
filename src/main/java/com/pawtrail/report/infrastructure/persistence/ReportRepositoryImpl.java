package com.pawtrail.report.infrastructure.persistence;

import com.pawtrail.common.exception.CustomException;
import com.pawtrail.report.domain.enums.ReportStatus;
import com.pawtrail.report.domain.enums.ReportType;
import com.pawtrail.report.domain.exception.ReportErrorCode;
import com.pawtrail.report.domain.model.Report;
import com.pawtrail.report.domain.repository.ReportRepository;
import com.pawtrail.report.infrastructure.persistence.jpa.ReportJpaRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

/**
 * 도메인이 선언한 저장소 약속을 JPA 로 구현합니다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepository {

    // V20 의 부분 유일 인덱스 이름
    // 이 이름으로 부딪힌 것만 중복으로 봄
    static final String PENDING_UNIQUE = "uq_report_pending";

    private final ReportJpaRepository reportJpaRepository;

    /**
     * 저장하고 그 자리에서 반영합니다.
     *
     * 그냥 저장하면 반영이 트랜잭션이 끝날 때까지 미뤄질 수 있습니다.
     * 식별자를 애플리케이션이 만들어 넣으므로 INSERT 가 커밋 때로 밀리는데,
     * 그러면 유일 인덱스에 부딪히는 시점도 함께 밀려 아래 변환이 걸리지 않고
     * 부르는 쪽은 처리 중인 같은 제보가 있다는 안내 대신 정체를 알 수 없는 서버 오류를 받습니다.
     * ingest 가 실행 기록을 만들 때 같은 이유로 이렇게 했습니다.
     *
     * 위반을 잡아 다른 예외로 다시 던집니다. 삼키지 않습니다.
     * 잡고 성공으로 끝내면 트랜잭션에 남은 롤백 표시 때문에 커밋이 거부됩니다.
     *
     * 부분 유일 인덱스에 부딪힌 것만 409 로 바꿉니다.
     * CHECK 위반처럼 다른 제약에 걸린 것은 우리 코드의 잘못이라 그대로 던져 500 이 나가게 둡니다.
     */
    @Override
    public Report saveNew(Report report) {
        try {
            return reportJpaRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException e) {
            if (hitPendingUnique(e)) {
                log.info("처리 중인 같은 제보가 방금 저장됐습니다: accountId={}, placeId={}, type={}",
                        report.getAccountId(), report.getPlaceId(), report.getReportType());
                throw new CustomException(ReportErrorCode.REPORT_ALREADY_PENDING, e);
            }
            throw e;
        }
    }

    /**
     * 부딪힌 제약이 부분 유일 인덱스인지 봅니다.
     *
     * 하이버네이트가 제약 이름을 뽑아 주면 그것으로 보고,
     * 못 뽑았으면 DB 가 준 메시지에 이름이 들어 있는지 봅니다.
     */
    private static boolean hitPendingUnique(DataIntegrityViolationException e) {
        Throwable cause = e.getCause();
        if (cause instanceof ConstraintViolationException violation
                && violation.getConstraintName() != null) {
            return PENDING_UNIQUE.equalsIgnoreCase(violation.getConstraintName());
        }
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains(PENDING_UNIQUE);
    }

    /**
     * 처리 중인 같은 제보를 찾습니다. 갈래는 ReportJpaRepository 의 주석에 있습니다.
     *
     * 후기 신고는 칸 이름이 늘 비어 있어 후기 식별자로만 가릅니다.
     */
    @Override
    public boolean existsPending(UUID accountId,
                                 UUID placeId,
                                 ReportType reportType,
                                 String fieldName,
                                 UUID targetReviewId) {
        if (targetReviewId != null) {
            return reportJpaRepository.existsByAccountIdAndPlaceIdAndReportTypeAndTargetReviewIdAndStatus(
                    accountId, placeId, reportType, targetReviewId, ReportStatus.PENDING);
        }
        if (fieldName != null) {
            return reportJpaRepository
                    .existsByAccountIdAndPlaceIdAndReportTypeAndFieldNameAndTargetReviewIdIsNullAndStatus(
                            accountId, placeId, reportType, fieldName, ReportStatus.PENDING);
        }
        return reportJpaRepository
                .existsByAccountIdAndPlaceIdAndReportTypeAndFieldNameIsNullAndTargetReviewIdIsNullAndStatus(
                        accountId, placeId, reportType, ReportStatus.PENDING);
    }

    @Override
    public long countSubmittedSince(UUID accountId, LocalDateTime since) {
        return reportJpaRepository.countByAccountIdAndCreatedAtGreaterThanEqual(accountId, since);
    }

    /**
     * 정렬은 메서드 이름이 정합니다. 쪽 요청에는 정렬을 싣지 않습니다.
     * 실으면 이름의 정렬 뒤에 덧붙어 차례가 흔들립니다.
     */
    @Override
    public Page<Report> findByAccountId(UUID accountId, int page, int size) {
        return reportJpaRepository.findByAccountIdOrderByCreatedAtDescIdDesc(
                accountId, PageRequest.of(page, size));
    }
}
