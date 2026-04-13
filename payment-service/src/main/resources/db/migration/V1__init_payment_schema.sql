CREATE TABLE IF NOT EXISTS payments
(
    payment_id        VARCHAR(36)    NOT NULL,
    order_id          VARCHAR(36)    NOT NULL,
    user_id           VARCHAR(36)    NOT NULL,
    amount            DECIMAL(19, 2) NOT NULL,
    status            VARCHAR(20)    NOT NULL,
    toss_payment_key  VARCHAR(200),
    method            VARCHAR(50),
    approved_at       VARCHAR(50),
    created_at        DATETIME(6),
    updated_at        DATETIME(6),
    PRIMARY KEY (payment_id),
    INDEX idx_payments_order_id (order_id),
    INDEX idx_payments_toss_key (toss_payment_key)
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
