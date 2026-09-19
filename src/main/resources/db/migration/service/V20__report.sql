-- 이 서비스의 첫 마이그레이션 스크립트입니다.
-- V1 부터 V19 는 공통 모듈이 사용하는 대역이므로 쓰지 않습니다.
--
-- 이미 적용된 스크립트는 수정하지 않습니다.
-- 내용이 바뀌면 체크섬이 달라져 다음 기동이 실패합니다.
-- 변경이 필요하면 다음 번호로 새 스크립트를 만듭니다.
--
-- report_db 에는 이 표 하나와 공통 대역의 outbox · processed_event 만 있습니다.
-- report 는 두 공통 표를 다 씁니다.
--   outbox           처리 결과(report.resolved) 발행
--   processed_event  탈퇴(account.withdrawn)를 한 번만 처리하는 장치
--
-- 장소 · 후기는 다른 서비스의 것이라 외래키를 걸지 않습니다.
-- 서비스가 갈려 DB 가 다르기 때문입니다.

-- =============================================================================
-- report
-- =============================================================================
-- 사용자가 올린 제보 한 건입니다. 장소 정보가 틀렸다는 제보와 불건전 후기 신고를 함께 담습니다.
--
-- 후기 신고를 따로 두지 않은 이유는 흐름이 제보와 같기 때문입니다.
-- 사용자가 올리고, 관리자가 승인 · 반려하고, 결과를 알림으로 받습니다.
-- 표를 나누면 사용자 화면과 관리자 목록이 통째로 하나 더 생깁니다.
--
-- 승인해도 장소 · 조건 값은 여기서 바뀌지 않습니다.
-- 실제 정정은 관리자가 place · policy 의 관리자 API 로 먼저 하고, 이 행은 처리 결과만 남깁니다.

CREATE TABLE report
(
    -- PK 는 모든 테이블이 uuid 입니다.
    -- 애플리케이션이 Hibernate 의 @UuidGenerator(style = VERSION_7) 로 생성해 넣으므로
    -- 여기에 기본값을 지정하지 않습니다.
    id                uuid           PRIMARY KEY,

    -- 제보한 계정입니다. auth_db 의 account.id 를 가리키나 외래키를 걸지 않습니다.
    -- 탈퇴하면 account.withdrawn 을 받아 이 계정의 행을 전부 지웁니다.
    account_id        uuid           NOT NULL,

    -- 어느 장소에 대한 제보인지입니다. place_db 의 place.id 를 가리킵니다.
    --
    -- 후기 신고도 채웁니다. 그 후기가 달린 장소입니다.
    -- 관리자가 신고된 후기를 볼 수 있는 자리가 장소 상세의 후기 카드뿐이라,
    -- 장소가 없으면 무엇을 신고했는지 확인할 길이 없습니다.
    --
    -- 제보를 받을 때 그 장소가 있는지 묻지 않습니다.
    -- 제보는 장소 상세에서만 시작되고, place 가 잠시 멈춰도 제보는 받아야 하기 때문입니다.
    -- 장소 이름은 목록을 만들 때 place 에서 받아 채웁니다.
    place_id          uuid           NOT NULL,

    -- 신고한 후기입니다. review_db 의 place_review.id 를 가리킵니다.
    -- 후기 신고(REVIEW_ABUSE)만 채우고 나머지 유형은 비웁니다.
    target_review_id  uuid,

    -- 제보 유형입니다.
    --   INFO_WRONG          전화 · 주소 · 영업시간 같은 장소 정보가 틀림
    --   CONDITION_WRONG     동반 조건이 실제와 다름
    --   PLACE_MERGED_WRONG  다른 장소가 잘못 묶임
    --   CLOSED              폐업함
    --   REVIEW_ABUSE        불건전 후기
    --
    -- CHECK 로 값 목록을 막지 않습니다.
    -- 유형이 늘면 마이그레이션이 하나 더 필요해지고, 값은 애플리케이션의 열거형이 막습니다.
    report_type       varchar(24)    NOT NULL,

    -- 틀렸다고 한 칸의 이름입니다. 장소 칸(tel · homepage …)이나 조건 칸(maxWeightKg …)입니다.
    -- 칸 목록은 place · policy 가 정하므로 여기서 값을 막지 않고 화면이 고른 이름을 그대로 담습니다.
    field_name        varchar(40),

    -- 사용자가 말하는 맞는 값입니다. "5kg 이하였습니다" 처럼 사람 말로 옵니다.
    -- 다른 장소가 잘못 묶였다는 제보(PLACE_MERGED_WRONG)는 잘못 묶인 소스의 코드를 담습니다.
    reported_value    varchar(500),

    -- 제보 본문입니다. 후기 본문과 같은 폭입니다.
    content           varchar(1000)  NOT NULL,

    -- 다녀온 날입니다. 달력에서 고르는 값이라 시각이 없습니다.
    -- 최근일수록 믿을 만한 제보라 관리자가 소스의 기준일과 견줘 봅니다.
    visited_at        date,

    -- 처리 상태입니다. PENDING · ACCEPTED · REJECTED
    status            varchar(16)    NOT NULL,

    -- 처리한 관리자 · 처리 시각 · 처리 메모입니다.
    -- 셋이 함께 채워지므로 처리 전에는 모두 비어 있습니다.
    --
    -- memo 는 칸에서는 비울 수 있고 처리 요청에서만 필수입니다.
    -- 제보를 받을 때는 관리자가 아직 쓰지 않았기 때문입니다.
    -- 결과 알림 본문에 그대로 실리므로 길면 안 됩니다.
    reviewed_by       varchar(45),
    reviewed_at       timestamp,
    memo              varchar(500),

    created_at        timestamp      NOT NULL,
    created_by        varchar(45)    NOT NULL,
    updated_at        timestamp      NOT NULL,
    updated_by        varchar(45)    NOT NULL,

    -- 공통 규약이라 두지만 쓰지 않습니다.
    -- 탈퇴한 계정의 제보는 행째 지우고, 사용자가 제보를 지우는 API 는 없습니다.
    deleted_at        timestamp,
    deleted_by        varchar(45),

    -- 후기 신고일 때만 후기 식별자가 있어야 합니다.
    -- 유형이 대상을 정하므로 둘이 어긋나면 그것은 버그이고, 그때는 DB 가 막습니다.
    CONSTRAINT chk_report_review_target
        CHECK ((report_type = 'REVIEW_ABUSE') = (target_review_id IS NOT NULL))
);

-- 내 제보 목록 (GET /api/v1/reports/me) 이 이 순서로 읽습니다.
-- 하루 상한을 셀 때도 계정 · 작성 시각으로 이 인덱스를 탑니다.
CREATE INDEX idx_report_account_created
    ON report (account_id, created_at DESC);

-- 관리자 목록 (GET /api/v1/admin/reports?status=) 이 이 순서로 읽습니다.
CREATE INDEX idx_report_status_created
    ON report (status, created_at DESC);

-- 처리 중인 같은 제보를 한 사람이 두 번 올리지 못하게 합니다.
--
-- 같은 제보 = 같은 사람 · 같은 장소 · 같은 유형 · 같은 칸 · 같은 후기
-- 다른 사람이 같은 제보를 올리는 것은 막지 않습니다. 여럿이 올리는 것 자체가 신호입니다.
-- 처리가 끝나면 다시 올릴 수 있도록 PENDING 인 행만 봅니다.
--
-- 애플리케이션이 먼저 조회로 거르고, 조회와 저장 사이에 끼어든 요청을 여기서 막습니다.
--
-- NULLS NOT DISTINCT 가 중요합니다.
-- 칸 이름과 후기 식별자는 비어 있는 유형이 많은데, 이것이 없으면 NULL 끼리는 서로 다르다고 보아
-- 폐업 제보 같은 것이 몇 번이고 들어갑니다. PostgreSQL 15 부터 쓸 수 있습니다.
CREATE UNIQUE INDEX uq_report_pending
    ON report (account_id, place_id, report_type, field_name, target_review_id)
    NULLS NOT DISTINCT
    WHERE status = 'PENDING';

COMMENT ON TABLE report IS '사용자 제보 · 후기 신고와 관리자 처리 결과';
COMMENT ON COLUMN report.place_id IS '제보 대상 장소 — 후기 신고는 그 후기가 달린 장소';
COMMENT ON COLUMN report.target_review_id IS '신고한 후기 — REVIEW_ABUSE 만 채움';
COMMENT ON COLUMN report.reported_value IS '사용자가 말하는 맞는 값 — PLACE_MERGED_WRONG 은 잘못 묶인 소스 코드';
COMMENT ON COLUMN report.memo IS '처리 메모 — 결과 알림 본문에 실림';
