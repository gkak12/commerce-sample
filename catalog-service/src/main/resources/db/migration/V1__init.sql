-- ── 카테고리 ──────────────────────────────────────────────────────────────────
CREATE TABLE categories
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    parent_id  BIGINT,
    depth      INT          NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL
);

-- ── 판매자 ────────────────────────────────────────────────────────────────────
CREATE TABLE sellers
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id       VARCHAR(36)  NOT NULL UNIQUE,
    user_id         VARCHAR(36)  NOT NULL UNIQUE,
    business_name   VARCHAR(100) NOT NULL,
    business_number VARCHAR(10)  NOT NULL UNIQUE,
    owner_name      VARCHAR(50)  NOT NULL,
    phone           VARCHAR(20)  NOT NULL,
    email           VARCHAR(100) NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL
);

-- ── 상품 ──────────────────────────────────────────────────────────────────────
CREATE TABLE products
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id  VARCHAR(36)    NOT NULL UNIQUE,
    seller_id   VARCHAR(36)    NOT NULL,
    category_id BIGINT         NOT NULL,
    name        VARCHAR(200)   NOT NULL,
    description TEXT,
    base_price  DECIMAL(12, 2) NOT NULL,
    status      VARCHAR(20)    NOT NULL,
    created_at  DATETIME(6)    NOT NULL,
    updated_at  DATETIME(6)    NOT NULL,
    INDEX idx_products_seller_id (seller_id),
    INDEX idx_products_category_status (category_id, status),
    INDEX idx_products_status (status)
);

-- ── 상품 이미지 ───────────────────────────────────────────────────────────────
CREATE TABLE product_images
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id   BIGINT       NOT NULL,
    url          VARCHAR(500) NOT NULL,
    is_thumbnail BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order   INT          NOT NULL DEFAULT 0,
    FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

-- ── 상품 옵션 ─────────────────────────────────────────────────────────────────
CREATE TABLE product_options
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id       BIGINT         NOT NULL,
    option_name      VARCHAR(50)    NOT NULL,
    option_value     VARCHAR(100)   NOT NULL,
    additional_price DECIMAL(10, 2) NOT NULL DEFAULT 0,
    stock_quantity   INT            NOT NULL DEFAULT 0,
    FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

-- ── 기본 카테고리 데이터 ───────────────────────────────────────────────────────
INSERT INTO categories (name, parent_id, depth, sort_order, created_at, updated_at)
VALUES ('패션', NULL, 1, 1, NOW(), NOW()),
       ('전자기기', NULL, 1, 2, NOW(), NOW()),
       ('식품', NULL, 1, 3, NOW(), NOW());
