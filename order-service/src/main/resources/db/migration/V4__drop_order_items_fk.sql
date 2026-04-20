-- order_items 외래키 제거
-- 대용량 트래픽 환경에서 FK 체크로 인한 락 경합 방지
-- 참조 무결성은 애플리케이션 레벨에서 보장
ALTER TABLE order_items
    DROP FOREIGN KEY fk_order_items_order;

ALTER TABLE order_items
    ADD INDEX idx_order_items_order_id (order_id);
