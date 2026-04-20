-- 마이페이지 주문 목록 조회 성능 개선
-- findByUserIdOrderByCreatedAtDesc 쿼리 풀 스캔 방지
CREATE INDEX idx_orders_user_created
    ON orders (user_id, created_at DESC);
