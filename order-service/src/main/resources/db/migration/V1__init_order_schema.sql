CREATE TABLE IF NOT EXISTS orders
(
    order_id     VARCHAR(36)    NOT NULL,
    user_id      VARCHAR(36)    NOT NULL,
    total_amount DECIMAL(19, 2) NOT NULL,
    status       VARCHAR(20)    NOT NULL,
    created_at   DATETIME(6),
    updated_at   DATETIME(6),
    PRIMARY KEY (order_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS order_items
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    order_id     VARCHAR(36)           NOT NULL,
    product_id   VARCHAR(36)           NOT NULL,
    product_name VARCHAR(100)          NOT NULL,
    quantity     INT                   NOT NULL,
    price        DECIMAL(19, 2)        NOT NULL,
    created_at   DATETIME(6),
    updated_at   DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (order_id)
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
