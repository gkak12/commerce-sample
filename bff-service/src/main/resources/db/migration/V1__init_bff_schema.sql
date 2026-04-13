CREATE TABLE IF NOT EXISTS users
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    user_id      VARCHAR(36)           NOT NULL,
    email        VARCHAR(255)          NOT NULL,
    name         VARCHAR(100)          NOT NULL,
    password     VARCHAR(255),
    provider     VARCHAR(20)           NOT NULL,
    provider_id  VARCHAR(255),
    role         VARCHAR(20)           NOT NULL,
    created_at   DATETIME(6),
    updated_at   DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE INDEX uk_users_user_id (user_id),
    INDEX idx_users_email_provider (email, provider)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
