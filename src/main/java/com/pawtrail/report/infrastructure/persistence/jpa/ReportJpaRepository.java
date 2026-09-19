package com.pawtrail.report.infrastructure.persistence.jpa;

import com.pawtrail.report.domain.enums.ReportStatus;
import com.pawtrail.report.domain.enums.ReportType;
import com.pawtrail.report.domain.model.Report;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 제보 표를 JPA 로 읽고 씁니다.
 *
 * 처리 중인 같은 제보를 찾는 조회가 갈래 셋으로 갈립니다.
 * 칸 이름과 후기 식별자가 비어 있을 수 있는데, 비어 있는 값을 매개변수로 넘기면
 * "= NULL" 비교가 되어 늘 거짓이 나오거나 형을 못 정해 실패할 수 있습니다.
 * 비어 있는 자리를 메서드 이름의 IsNull 로 적어 두면 매개변수로 null 이 넘어가지 않습니다.
 * 어느 갈래를 부를지는 ReportRepositoryImpl 이 고릅니다.
 */
public interface ReportJpaRepository extends JpaRepository<Report, UUID> {

    // 장소 제보 · 칸 이름 있음 (정보 · 조건 제보)
    boolean existsByAccountIdAndPlaceIdAndReportTypeAndFieldNameAndTargetReviewIdIsNullAndStatus(
            UUID accountId, UUID placeId, ReportType reportType, String fieldName, ReportStatus status);

    // 장소 제보 · 칸 이름 없음 (폐업 · 잘못 묶임 · 칸을 모르는 조건 제보)
    boolean existsByAccountIdAndPlaceIdAndReportTypeAndFieldNameIsNullAndTargetReviewIdIsNullAndStatus(
            UUID accountId, UUID placeId, ReportType reportType, ReportStatus status);

    // 후기 신고 — 칸 이름은 이 유형에서 늘 비어 있음
    boolean existsByAccountIdAndPlaceIdAndReportTypeAndTargetReviewIdAndStatus(
            UUID accountId, UUID placeId, ReportType reportType, UUID targetReviewId, ReportStatus status);

    long countByAccountIdAndCreatedAtGreaterThanEqual(UUID accountId, LocalDateTime since);

    Page<Report> findByAccountIdOrderByCreatedAtDescIdDesc(UUID accountId, Pageable pageable);

    // 관리자 목록 — 상태를 고른 경우 · 전부
    Page<Report> findByStatusOrderByCreatedAtDescIdDesc(ReportStatus status, Pageable pageable);

    Page<Report> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    // 처리할 때 한 건을 잠가 읽음 (SELECT … FOR UPDATE)
    // place 가 수집 대기를 처리할 때와 같은 모양
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Report r WHERE r.id = :id")
    Optional<Report> findByIdForUpdate(@Param("id") UUID id);
}
