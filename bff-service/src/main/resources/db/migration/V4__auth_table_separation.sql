-- ============================================================
-- V4: 인증 테이블 분리 (일반회원 / 판매자 / 관리자)
--
-- 변경 사항:
--   1. users 테이블: role 컬럼 제거, email UNIQUE 추가
--   2. sellers 테이블 신규 생성 (판매자 인증 전용)
--   3. admins  테이블 신규 생성 (관리자 인증 전용)
--   4. 최초 관리자 계정 INSERT (이후 계정은 기존 관리자가 생성)
-- ============================================================


-- ── 1. users 테이블 변경 ──────────────────────────────────────────────────────

-- role 컬럼 제거 (역할은 테이블로 구분)
ALTER TABLE users DROP COLUMN role;

-- email UNIQUE 추가
ALTER TABLE users ADD UNIQUE INDEX uk_users_email (email);


-- ── 2. sellers 테이블 ─────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS sellers
(
    id           BIGINT          NOT NULL AUTO_INCREMENT   COMMENT '판매자 PK',
    seller_id    VARCHAR(36)     NOT NULL                  COMMENT '판매자 UUID',
    email        VARCHAR(255)    NOT NULL                  COMMENT '로그인 이메일',
    password     VARCHAR(255)    NOT NULL                  COMMENT 'BCrypt 해시 비밀번호',
    name         VARCHAR(100)    NOT NULL                  COMMENT '판매자 이름',
    created_at   DATETIME(6),
    updated_at   DATETIME(6),

    PRIMARY KEY (id),
    UNIQUE INDEX uk_sellers_seller_id (seller_id),
    UNIQUE INDEX uk_sellers_email     (email)
)   ENGINE  = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci
    COMMENT = '판매자 인증 테이블 (이메일/비밀번호 전용, 소셜 로그인 미지원)';


-- ── 3. admins 테이블 ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS admins
(
    id                       BIGINT          NOT NULL AUTO_INCREMENT   COMMENT '관리자 PK',
    admin_id                 VARCHAR(36)     NOT NULL                  COMMENT '관리자 UUID',
    email                    VARCHAR(255)    NOT NULL                  COMMENT '로그인 이메일',
    password                 VARCHAR(255)    NOT NULL                  COMMENT 'BCrypt 해시 비밀번호',
    name                     VARCHAR(100)    NOT NULL                  COMMENT '관리자 이름',
    password_change_required BOOLEAN         NOT NULL DEFAULT TRUE     COMMENT '임시 비밀번호 변경 필요 여부',
    created_by               VARCHAR(36)     NULL                      COMMENT '생성한 관리자 admin_id (최초 관리자는 NULL)',
    created_at               DATETIME(6),
    updated_at               DATETIME(6),

    PRIMARY KEY (id),
    UNIQUE INDEX uk_admins_admin_id (admin_id),
    UNIQUE INDEX uk_admins_email    (email)
)   ENGINE  = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci
    COMMENT = '관리자 인증 테이블 (이메일/비밀번호 전용, 소셜 로그인 미지원)';


-- ── 4. 최초 관리자 계정 ───────────────────────────────────────────────────────
--
-- 임시 비밀번호: Admin1234!
-- BCrypt hash (cost=10): $2a$10$N.mMjQqCMsJH4jQ7KKyXWOt7MkSFlUJWXHK0fFzKxl7AJgqV5eH8W
--
-- ※ 첫 로그인 후 POST /api/admin/auth/password 로 반드시 변경할 것
-- ※ 운영 배포 전 이 비밀번호를 직접 UPDATE하거나 별도 migration으로 교체 필요

INSERT INTO admins (admin_id, email, password, name, password_change_required, created_by)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@commerce.com',
    '$2a$10$N.mMjQqCMsJH4jQ7KKyXWOt7MkSFlUJWXHK0fFzKxl7AJgqV5eH8W',
    '시스템 관리자',
    TRUE,
    NULL
);
