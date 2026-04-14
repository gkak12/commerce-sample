CREATE TABLE IF NOT EXISTS product_stock
(
    product_id VARCHAR(100) NOT NULL,
    quantity   BIGINT       NOT NULL DEFAULT 0,
    synced_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (product_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Redis 재고 스냅샷 — 주기적으로 Redis 현재값을 동기화';
