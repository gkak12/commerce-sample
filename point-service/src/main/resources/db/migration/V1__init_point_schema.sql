CREATE TABLE IF NOT EXISTS point_wallets
(
    user_id     VARCHAR(36) NOT NULL,
    total_point BIGINT      NOT NULL DEFAULT 0,
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    PRIMARY KEY (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS points
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    user_id      VARCHAR(36)           NOT NULL,
    order_id     VARCHAR(36)           NOT NULL,
    earned_point BIGINT                NOT NULL,
    type         VARCHAR(20)           NOT NULL,
    created_at   DATETIME(6),
    updated_at   DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_points_user_id (user_id),
    INDEX idx_points_order_id (order_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS outbox_events
(
    id            VARCHAR(36)  NOT NULL,
    aggregate_id  VARCHAR(36)  NOT NULL,
    topic         VARCHAR(100) NOT NULL,
    payload       TEXT         NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    retry_count   INT          NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NOT NULL,
    published_at  DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_outbox_status_created (status, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
