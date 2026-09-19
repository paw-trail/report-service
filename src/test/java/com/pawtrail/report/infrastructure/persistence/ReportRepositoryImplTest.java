package com.pawtrail.report.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pawtrail.common.exception.CustomException;
import com.pawtrail.report.IntegrationTestSupport;
import com.pawtrail.report.domain.enums.ReportStatus;
import com.pawtrail.report.domain.enums.ReportType;
import com.pawtrail.report.domain.exception.ReportErrorCode;
import com.pawtrail.report.domain.model.Report;
import com.pawtrail.report.domain.repository.ReportRepository;
import com.pawtrail.report.infrastructure.persistence.jpa.ReportJpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * V20 의 제약과 저장소 조회를 실제 PostgreSQL 로 검사합니다.
 *
 * 트랜잭션을 걸지 않습니다. 저장마다 실제로 커밋해야 인덱스에 부딪히는 자리를 볼 수 있어서입니다.
 * 대신 검사마다 표를 비웁니다.
 */
class ReportRepositoryImplTest extends IntegrationTestSupport {

    private static final UUID ACCOUNT = UUID.fromString("01999999-0000-7000-8000-000000000001");
    private static final UUID OTHER_ACCOUNT = UUID.fromString("01999999-0000-7000-8000-000000000002");
    private static final UUID PLACE = UUID.fromString("01999999-0000-7000-8000-00000000aaaa");
    private static final UUID OTHER_PLACE = UUID.fromString("01999999-0000-7000-8000-00000000cccc");
    private static final UUID REVIEW = UUID.fromString("01999999-0000-7000-8000-00000000bbbb");
    private static final UUID OTHER_REVIEW = UUID.fromString("01999999-0000-7000-8000-00000000dddd");

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportJpaRepository reportJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanUp() {
        reportJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("처리 중인 같은 제보는 인덱스가 막고 409 로 바뀐다 — 칸이 비어 있어도")
    void 인덱스가_막는다() {
        reportRepository.saveNew(closed(ACCOUNT, PLACE));

        assertThatThrownBy(() -> reportRepository.saveNew(closed(ACCOUNT, PLACE)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ReportErrorCode.REPORT_ALREADY_PENDING));
        assertThat(reportJpaRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("반려한 뒤에는 같은 제보를 다시 받고, 다른 사람의 같은 제보도 받는다")
    void 다시_받는다() {
        Report first = reportRepository.saveNew(closed(ACCOUNT, PLACE));
        jdbcTemplate.update("UPDATE report SET status = 'REJECTED' WHERE id = ?", first.getId());

        reportRepository.saveNew(closed(ACCOUNT, PLACE));
        reportRepository.saveNew(closed(OTHER_ACCOUNT, PLACE));

        assertThat(reportJpaRepository.count()).isEqualTo(3L);
    }

    @Test
    @DisplayName("같은 제보 찾기는 후기 · 칸 있음 · 칸 없음 세 갈래가 모두 맞게 답한다")
    void 같은_제보_찾기() {
        reportRepository.saveNew(closed(ACCOUNT, PLACE));
        reportRepository.saveNew(Report.submit(
                ACCOUNT, PLACE, ReportType.INFO_WRONG, "tel", null, "번호가 달라요", null, null));
        reportRepository.saveNew(Report.submit(
                ACCOUNT, PLACE, ReportType.REVIEW_ABUSE, null, null, "욕설이 있어요", null, REVIEW));

        assertThat(reportRepository.existsPending(ACCOUNT, PLACE, ReportType.CLOSED, null, null)).isTrue();
        assertThat(reportRepository.existsPending(ACCOUNT, PLACE, ReportType.CONDITION_WRONG, null, null)).isFalse();
        assertThat(reportRepository.existsPending(ACCOUNT, PLACE, ReportType.INFO_WRONG, "tel", null)).isTrue();
        assertThat(reportRepository.existsPending(ACCOUNT, PLACE, ReportType.INFO_WRONG, "homepage", null)).isFalse();
        assertThat(reportRepository.existsPending(ACCOUNT, PLACE, ReportType.REVIEW_ABUSE, null, REVIEW)).isTrue();
        assertThat(reportRepository.existsPending(ACCOUNT, PLACE, ReportType.REVIEW_ABUSE, null, OTHER_REVIEW)).isFalse();
        assertThat(reportRepository.existsPending(OTHER_ACCOUNT, PLACE, ReportType.CLOSED, null, null)).isFalse();
    }

    @Test
    @DisplayName("하루 상한은 그 사람이 since 뒤에 올린 것만 센다")
    void 세기() {
        reportRepository.saveNew(closed(ACCOUNT, PLACE));
        reportRepository.saveNew(closed(ACCOUNT, OTHER_PLACE));
        reportRepository.saveNew(closed(OTHER_ACCOUNT, PLACE));
        jdbcTemplate.update(
                "UPDATE report SET created_at = created_at - interval '1 day' WHERE account_id = ? AND place_id = ?",
                ACCOUNT, OTHER_PLACE);

        assertThat(reportRepository.countSubmittedSince(ACCOUNT, LocalDate.now().atStartOfDay())).isEqualTo(1L);
    }

    @Test
    @DisplayName("내 목록은 최신순으로 쪽을 나누고 남의 제보는 안 나온다")
    void 내_목록() {
        Report older = reportRepository.saveNew(closed(ACCOUNT, PLACE));
        Report newer = reportRepository.saveNew(closed(ACCOUNT, OTHER_PLACE));
        reportRepository.saveNew(closed(OTHER_ACCOUNT, PLACE));

        Page<Report> first = reportRepository.findByAccountId(ACCOUNT, 0, 1);
        Page<Report> second = reportRepository.findByAccountId(ACCOUNT, 1, 1);

        assertThat(first.getTotalElements()).isEqualTo(2L);
        assertThat(first.getContent().get(0).getId()).isEqualTo(newer.getId());
        assertThat(second.getContent().get(0).getId()).isEqualTo(older.getId());
    }

    @Test
    @DisplayName("후기 신고인데 후기 식별자가 없으면 DB 가 막는다")
    void 체크() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO report (id, account_id, place_id, report_type, content, status,
                                    created_at, created_by, updated_at, updated_by)
                VALUES (?, ?, ?, 'REVIEW_ABUSE', '욕설이 있어요', 'PENDING', now(), 'test', now(), 'test')
                """, UUID.randomUUID(), ACCOUNT, PLACE))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("관리자 목록은 상태를 주면 그 상태만, 비우면 전부를 최신순으로 준다")
    void 관리자_목록() {
        Report first = reportRepository.saveNew(closed(ACCOUNT, PLACE));
        Report second = reportRepository.saveNew(closed(OTHER_ACCOUNT, PLACE));
        jdbcTemplate.update("UPDATE report SET status = 'REJECTED' WHERE id = ?", first.getId());

        Page<Report> pending = reportRepository.findForAdmin(ReportStatus.PENDING, 0, 20);
        Page<Report> all = reportRepository.findForAdmin(null, 0, 20);

        assertThat(pending.getTotalElements()).isEqualTo(1L);
        assertThat(pending.getContent().get(0).getId()).isEqualTo(second.getId());
        assertThat(all.getTotalElements()).isEqualTo(2L);
        assertThat(all.getContent().get(0).getId()).isEqualTo(second.getId());
    }

    @Test
    @DisplayName("잠금 조회는 쓰기 트랜잭션 안에서 행을 돌려주고, 없는 식별자면 비어 있다")
    void 잠금_조회() {
        Report saved = reportRepository.saveNew(closed(ACCOUNT, PLACE));
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Optional<Report> found = tx.execute(status -> reportRepository.findByIdForUpdate(saved.getId()));
        Optional<Report> missing = tx.execute(status -> reportRepository.findByIdForUpdate(UUID.randomUUID()));

        assertThat(found.isPresent()).isTrue();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(missing.isPresent()).isFalse();
    }

    private static Report closed(UUID accountId, UUID placeId) {
        return Report.submit(accountId, placeId, ReportType.CLOSED, null, null, "문을 닫았어요", null, null);
    }
}
