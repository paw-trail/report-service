package com.pawtrail.report.domain.repository;

import com.pawtrail.report.domain.enums.ReportStatus;
import com.pawtrail.report.domain.enums.ReportType;
import com.pawtrail.report.domain.model.Report;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;

/**
 * 제보를 저장하고 찾아오는 약속입니다.
 *
 * 이 인터페이스에는 JPA 라는 단어가 나오지 않습니다.
 * 무엇을 할 수 있는지만 적고 어떻게 하는지는 infrastructure 가 정합니다.
 */
public interface ReportRepository {

    /**
     * 새 제보를 저장하고 그 자리에서 DB 에 반영합니다.
     *
     * 처리 중인 같은 제보가 이미 있으면 REPORT_ALREADY_PENDING 으로 거절합니다.
     * 부르는 쪽이 먼저 조회로 거르지만, 조회와 저장 사이에 같은 요청이 끼어들면
     * 부분 유일 인덱스가 막고 그 위반을 이 메서드가 같은 오류로 바꿉니다.
     */
    Report saveNew(Report report);

    /**
     * 같은 사람이 처리 중인 같은 제보가 있는지 봅니다.
     *
     * 같은 제보 = 같은 장소 · 같은 유형 · 같은 칸 · 같은 후기
     * 칸 이름과 후기 식별자는 비어 있을 수 있고, 비어 있는 것끼리도 같은 것으로 봅니다.
     * 인덱스의 NULLS NOT DISTINCT 와 같은 기준입니다.
     */
    boolean existsPending(UUID accountId,
                          UUID placeId,
                          ReportType reportType,
                          String fieldName,
                          UUID targetReviewId);

    /**
     * 그 사람이 since 이후에 올린 제보 수를 셉니다. 하루 상한을 볼 때 씁니다.
     */
    long countSubmittedSince(UUID accountId, LocalDateTime since);

    /**
     * 그 사람의 제보를 최신순으로 한 쪽 읽습니다.
     *
     * 작성 시각이 같으면 식별자 순으로 가릅니다.
     * 순서가 정해져 있지 않으면 쪽을 넘길 때 같은 행이 두 번 나오거나 한 행이 빠집니다.
     * 식별자가 UUID 버전 7 이라 그 순서가 곧 만들어진 순서입니다.
     */
    Page<Report> findByAccountId(UUID accountId, int page, int size);

    /**
     * 관리자 목록을 최신순으로 한 쪽 읽습니다.
     *
     * 상태를 주면 그 상태만, 비워 두면 전부입니다. 차례는 내 목록과 같습니다.
     */
    Page<Report> findForAdmin(ReportStatus status, int page, int size);

    /**
     * 처리하려고 한 건을 잠가 읽습니다.
     *
     * 두 관리자가 같은 카드를 동시에 누르면 뒤에 온 쪽이 앞의 처리가 끝날 때까지 기다렸다가
     * 이미 처리된 상태를 보게 됩니다. 잠그지 않으면 둘 다 처리 전으로 읽고 결과가 두 번 나갑니다.
     *
     * 쓰기 트랜잭션 안에서만 부릅니다. 읽기 전용 트랜잭션에서는 PostgreSQL 이 잠금 조회를 거절합니다.
     */
    Optional<Report> findByIdForUpdate(UUID reportId);
}
