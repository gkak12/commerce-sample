# Commerce Sample — MSA 쇼핑몰 백엔드

Spring Boot 기반 마이크로서비스 아키텍처(MSA) 쇼핑몰 백엔드 프로젝트입니다.  
Kafka 이벤트 기반 Saga 패턴, gRPC 내부 통신, Redis 재고 관리 등 실무 아키텍처 패턴을 적용했습니다.

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.3 |
| Build | Gradle (Kotlin DSL) |
| Database | MySQL 8.0, Flyway |
| Cache | Redis (Lettuce) |
| Message Broker | Apache Kafka |
| Internal API | gRPC 1.62.2 |
| Auth | JWT (jjwt 0.12.3), OAuth2 (Google, Naver) |
| Resilience | Resilience4j 2.2.0 |
| Monitoring | Actuator, Micrometer, Prometheus, Grafana |
| Docs | SpringDoc OpenAPI 3 (Swagger) |
| ORM | Spring Data JPA, QueryDSL 5.1.0 |
| Mail | Spring Mail (Naver SMTP) |

---

## 서비스 구성

```
commerce-sample
├── common           # 공통 모듈 (Kafka 토픽, 이벤트, gRPC proto, 분산 추적)
├── bff-service      # API Gateway / BFF  (포트: 8080)
├── catalog-service  # 상품/판매자 서비스  (포트: 8085, gRPC: 9095)
├── order-service    # 주문 서비스         (포트: 8081, gRPC: 9091)
├── payment-service  # 결제 서비스         (포트: 8082)
├── delivery-service # 배송 서비스         (포트: 8083, gRPC: 9093)
└── point-service    # 포인트 서비스        (포트: 8084, gRPC: 9094)
```

### 서비스별 역할

| 서비스 | 역할 |
|--------|------|
| **bff-service** | 외부 REST API, 인증/인가, Redis 재고 관리, Kafka 이벤트 발행, gRPC 클라이언트 |
| **catalog-service** | 상품 조회/관리, 판매자 등록/승인, gRPC 카탈로그 서버 |
| **order-service** | 주문 생성/상태관리, Saga 보상 처리, gRPC 주문 조회 서버 |
| **payment-service** | 토스페이먼츠 연동, 결제 승인/실패 이벤트 발행 |
| **delivery-service** | 배송 레코드 관리, gRPC 배송 조회 서버 |
| **point-service** | 포인트 적립/조회, gRPC 포인트 잔액 서버 |

---

## 시스템 아키텍처

```
[Client]
    │ REST
    ▼
[bff-service :8080]
    │                           ┌─ gRPC ─► [catalog-service :9095]
    │                           ├─ gRPC ─► [order-service :9091]
    │                           ├─ gRPC ─► [delivery-service :9093]
    │                           └─ gRPC ─► [point-service :9094]
    │
    │ Kafka
    ├─── order.created ──────────────────► [order-service]
    │                                           │ order.confirmed
    │                                           ▼
    │                                    [payment-service]
    │                                    [delivery-service]
    │                                    [point-service]
    │
    └─── order.cancel.requested ─────────► [order-service]
```

---

## 인증 구조

3개의 회원 유형을 **별도 테이블과 컨트롤러로 완전 분리**하여 관리합니다.

### 테이블 분리

| 유형 | 테이블 | 컨트롤러 | 기본 경로 |
|------|--------|----------|-----------|
| 일반 회원 | `users` | `AuthController` | `/api/auth` |
| 판매자 | `sellers` | `SellerAuthController` | `/api/auth/seller` |
| 관리자 | `admins` | `AdminAuthController` | `/api/admin/auth` |

### JWT 라우팅

토큰에 `userType` 클레임을 포함하여 요청 시 적절한 테이블로 라우팅합니다.

```
JwtAuthenticationFilter
    ├─ userType=USER   → CustomUserDetailsService   → users 테이블
    ├─ userType=SELLER → SellerDetailsService       → sellers 테이블
    └─ userType=ADMIN  → AdminDetailsService        → admins 테이블
```

### 쿠키 분리

각 유형별로 독립적인 Refresh Token 쿠키를 사용합니다.

| 유형 | 쿠키 이름 | 경로 |
|------|-----------|------|
| 일반 회원 | `refreshToken` | `/api/auth` |
| 판매자 | `sellerRefreshToken` | `/api/auth/seller` |
| 관리자 | `adminRefreshToken` | `/api/admin/auth` |

---

## 주요 아키텍처 패턴

### 1. Transactional Outbox 패턴
DB 저장과 Kafka 전송의 원자성을 보장합니다.

```
비즈니스 로직 실행
    ↓
orders 테이블 + outbox_events 테이블 → 하나의 트랜잭션으로 저장
    ↓
OutboxEventPublisher (5초 주기) → Kafka 전송 → PUBLISHED 처리
```

### 2. Saga 패턴 (Choreography 방식)
분산 트랜잭션을 이벤트 기반으로 처리합니다.

**정상 흐름**
```
order.created → order.confirmed → payment.completed → order.completed → point.earned
```

**보상 흐름 (결제 실패)**
```
payment.failed → 주문 취소 → order.cancelled → stock.restore → Redis 재고 복구
```

### 3. Redis 원자적 재고 관리 및 순서 보장
Redis의 단일 스레드 명령 처리 구조를 활용해 동시 요청에서도 순서와 정확성을 보장합니다.

```
동시에 사용자 100명이 같은 상품 요청
    ↓
Redis DECR stock:{productId} (단일 스레드, 원자적 처리)
    ↓
요청 도착 순서대로 하나씩 처리
    ↓
재고 1개: 1번째 요청 → 잔여 0 → 성공
          2번째 요청 → 잔여 -1 → 실패 (롤백)
          3번째 요청 → 잔여 -2 → 실패 (롤백)
```

- `DECR` / `INCR` 는 Redis 원자적 연산 → 별도 분산 락 불필요
- 재고 데이터 유실 방지: 스냅샷 스케줄러(30초 주기)로 Redis → MySQL 동기화
- 서비스 재시작 시 `StockRedisLoader`가 MySQL → Redis 로드

### 4. Kafka 이벤트 순서 보장
같은 주문의 이벤트는 항상 같은 파티션으로 라우팅되어 순서가 보장됩니다.

```
orderId를 Kafka 메시지 키로 사용
kafkaTemplate.send("order.created", orderId, event)
                                    ↑ 키

orderId=A → 항상 partition 0 → 순서 보장
  [주문생성] → [결제완료] → [배송시작] (offset 순서대로 처리)

orderId=B → 항상 partition 1 → 순서 보장 (A와 독립적으로 병렬 처리)
```

- 파티션 수: 3개 (병렬 처리)
- 파티션 내부: offset 순서대로 처리 → 같은 주문 이벤트 순서 보장
- EOS 설정(`enable.idempotence=true`, `acks=all`)으로 재시도 시 순서 역전 방지

### 5. Kafka 멱등성 및 메시지 유실 방지

**Producer 측 — 메시지 유실 방지**

| 설정 | 값 | 효과 |
|------|-----|------|
| `acks` | `all` | 모든 브로커 저장 확인 후 완료 처리 |
| `enable.idempotence` | `true` | 재시도 시 중복 메시지를 브로커가 자동 제거 |
| `retries` | `Integer.MAX_VALUE` | 전송 실패 시 무한 재시도 |
| Outbox 패턴 | - | DB 저장과 Kafka 전송 원자적 처리 |

**Consumer 측 — 멱등성 (중복 처리 방지)**

Kafka는 장애 복구 시 동일 메시지를 재전달할 수 있습니다.  
각 도메인 서비스 Consumer는 처리 전 **DB 조회로 중복 여부를 확인**합니다.

```java
// order-service: 주문 생성
if (orderRepository.existsById(event.getOrderId())) {
    log.warn("[Order] Duplicate event ignored. orderId={}", event.getOrderId());
    return;
}

// payment-service: 결제 레코드 생성
if (paymentRepository.findByOrderId(event.getOrderId()).isPresent()) {
    log.warn("[Payment] Duplicate event ignored. orderId={}", event.getOrderId());
    return;
}

// delivery-service: 배송 생성
if (deliveryRepository.existsByOrderId(event.getOrderId())) {
    log.warn("[Delivery] Duplicate event ignored. orderId={}", event.getOrderId());
    return;
}

// point-service: 포인트 적립
if (pointRepository.existsByOrderId(event.getOrderId())) {
    log.warn("[Point] Duplicate event ignored. orderId={}", event.getOrderId());
    return;
}
```

| 서비스 | 멱등성 기준 키 | 처리 방식 |
|--------|-------------|---------|
| order-service | orderId | DB 존재 여부 확인 후 중복 무시 |
| payment-service | orderId | DB 존재 여부 확인 후 중복 무시 |
| delivery-service | orderId | DB 존재 여부 확인 후 중복 무시 |
| point-service | orderId | DB 존재 여부 확인 후 중복 무시 |

**Consumer 측 — 처리 실패 대응 (DLT)**

```
메시지 처리 실패
  → 2초 후 재시도 → 4초 후 재시도 → 8초 후 재시도
  → 3회 모두 실패 → DLT(Dead Letter Topic)로 이동
  → DLT Consumer가 별도 처리 (로그 기록, 수동 재처리)
```

**Consumer 측 — 격리 수준**

- `isolation.level=read_committed`: 트랜잭션 커밋이 완료된 메시지만 읽음 (미완료 메시지 차단)

### 6. Circuit Breaker (Resilience4j)
gRPC 및 Redis 호출 장애 시 연쇄 장애를 방지합니다.

```
적용 대상: CatalogGrpcClient, OrderGrpcClient, PointGrpcClient, DeliveryGrpcClient, StockRedisService

설정 (gRPC 서비스 공통):
  - 슬라이딩 윈도우: 10회
  - 실패율 임계값: 50%
  - 느린 호출 기준: 3초 이상
  - 느린 호출 임계값: 50%
  - 서킷 오픈 대기 시간: 10초
  - 재시도: 최대 3회 (지수 백오프: 500ms → 1s → 2s)
```

상태 전이 시 `CircuitBreakerEventListener`가 로그를 출력합니다.

```
CLOSED → OPEN     : ERROR (장애 감지, fallback 응답 중)
OPEN   → HALF_OPEN: WARN  (복구 시도 중)
HALF_OPEN → CLOSED: INFO  (정상 복구 완료)
```

### 7. 분산 추적 (MDC / traceId)

HTTP 요청부터 Kafka Consumer까지 동일한 `traceId`로 전체 흐름을 추적합니다.

```
HTTP 요청 진입
    → TraceIdFilter: MDC["traceId"] 생성 및 응답 헤더(X-Trace-Id)에 포함
    → KafkaProducerTraceInterceptor: MDC에서 traceId 읽어 Kafka 메시지 헤더에 삽입
    → (Kafka 브로커)
    → KafkaConsumerConfig.traceRecordInterceptor: 헤더에서 traceId 추출 → MDC 세팅
    → Consumer 로직의 모든 로그에 동일 traceId 자동 포함
    → afterRecord(): MDC.remove()
```

로그 예시:
```
2026-05-27 10:00:01 [a1b2c3d4] INFO  OrderService - 주문 처리 시작
2026-05-27 10:00:01 [a1b2c3d4] INFO  KafkaProducer - order.created 발행
2026-05-27 10:00:02 [a1b2c3d4] INFO  OrderConsumer - order.created 수신
```

---

## 인프라 구성

로컬 실행 기준 (Docker 권장)

| 인프라 | 포트 | 용도 |
|--------|------|------|
| MySQL | 3306 | 서비스별 DB (6개) |
| Kafka | 9092 | 이벤트 브로커 |
| Redis | 6379 | 재고 캐시 |
| Prometheus | 9090 | 메트릭 수집 |
| Grafana | 3000 | 모니터링 대시보드 |

### 데이터베이스

| 서비스 | DB명 |
|--------|------|
| bff-service | bff_db |
| catalog-service | commerce_catalog |
| order-service | commerce_order |
| payment-service | commerce_payment |
| delivery-service | commerce_delivery |
| point-service | commerce_point |

---

## API 명세

### 일반 회원 인증 API

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/auth/signup` | 회원가입 | 불필요 |
| POST | `/api/auth/login` | 로그인 | 불필요 |
| POST | `/api/auth/refresh` | 토큰 갱신 | Refresh Token |
| POST | `/api/auth/logout` | 로그아웃 | 필요 |
| POST | `/api/auth/password` | 비밀번호 변경 | 필요 |

### 판매자 인증 API

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/auth/seller/signup` | 판매자 회원가입 | 불필요 |
| POST | `/api/auth/seller/login` | 판매자 로그인 | 불필요 |
| POST | `/api/auth/seller/refresh` | 토큰 갱신 | Refresh Token |
| POST | `/api/auth/seller/logout` | 로그아웃 | 필요 |
| POST | `/api/auth/seller/password` | 비밀번호 변경 | 필요 |

### 관리자 인증 API

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/admin/accounts` | 관리자 계정 생성 (임시 비밀번호 이메일 발송) | ADMIN |
| POST | `/api/admin/auth/login` | 관리자 로그인 | 불필요 |
| POST | `/api/admin/auth/refresh` | 토큰 갱신 | Refresh Token |
| POST | `/api/admin/auth/logout` | 로그아웃 | 필요 |
| POST | `/api/admin/auth/password` | 비밀번호 변경 | 필요 |

### 주문 API

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/orders` | 주문 생성 (재고 선점 + Kafka 발행) | 필요 |
| DELETE | `/api/orders/{orderId}` | 주문 취소 요청 | 필요 |

### 마이페이지 API (bff-service → gRPC)

| Method | URI | 설명 | 내부 통신 |
|--------|-----|------|----------|
| GET | `/api/my/orders` | 내 주문 목록 | gRPC → order-service |
| GET | `/api/my/orders/{orderId}` | 주문 상세 + 배송 상태 | gRPC → order, delivery |
| GET | `/api/my/points` | 포인트 잔액 조회 | gRPC → point-service |

### 상품 API (bff-service → catalog-service gRPC)

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| GET | `/api/products` | 상품 목록 조회 | 불필요 |
| GET | `/api/products/{productId}` | 상품 상세 조회 | 불필요 |

### 판매자 API (bff-service → catalog-service gRPC)

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/sellers/register` | 판매자 사업자 정보 등록 | SELLER |
| GET | `/api/sellers/me` | 내 판매자 정보 조회 | SELLER |
| PUT | `/api/sellers/me` | 판매자 정보 수정 | SELLER |
| POST | `/api/sellers/products` | 상품 등록 | SELLER |
| PUT | `/api/sellers/products/{productId}` | 상품 수정 | SELLER |
| DELETE | `/api/sellers/products/{productId}` | 상품 삭제 | SELLER |
| GET | `/api/sellers/products` | 내 상품 목록 | SELLER |

### 재고 API

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/stocks/{productId}/init` | 재고 초기화 | ADMIN |
| GET | `/api/stocks/{productId}` | 재고 조회 | ADMIN |

### 결제 API (payment-service)

| Method | URI | 설명 |
|--------|-----|------|
| POST | `/api/payments/confirm` | 토스페이먼츠 결제 승인 |
| POST | `/api/payments/webhook` | 토스페이먼츠 웹훅 수신 |

---

## Kafka 토픽

| 토픽 | 발행자 | 구독자 | 파티션 |
|------|--------|--------|--------|
| `order.created` | bff-service | order-service | 3 |
| `order.confirmed` | order-service | payment, delivery, point | 3 |
| `order.completed` | order-service | bff-service (메일) | 3 |
| `order.cancelled` | order-service | bff-service (메일) | 3 |
| `order.cancel.requested` | bff-service | order-service | 3 |
| `payment.completed` | payment-service | order, delivery, point | 3 |
| `payment.failed` | payment-service | order-service | 3 |
| `delivery.started` | delivery-service | - | 3 |
| `point.earned` | point-service | - | 3 |
| `stock.restore` | order-service | bff-service | 3 |
| `seller.approved` | catalog-service | bff-service | 3 |
| `*.DLT` | 각 서비스 (3회 실패 시) | 각 서비스 DLT Consumer | 3 |

---

## gRPC 서비스

```protobuf
// catalog_service.proto
service CatalogService {
    // 상품 조회 (공개)
    rpc GetProduct(GetProductRequest) returns (GetProductResponse);
    rpc GetProductList(GetProductListRequest) returns (GetProductListResponse);

    // 판매자 관리
    rpc RegisterSeller(RegisterSellerRequest) returns (SellerResponse);
    rpc GetMySellerInfo(GetMySellerInfoRequest) returns (SellerResponse);
    rpc UpdateSellerInfo(UpdateSellerInfoRequest) returns (SellerResponse);

    // 상품 관리 (판매자)
    rpc CreateProduct(CreateProductRequest) returns (ProductResponse);
    rpc UpdateProduct(UpdateProductRequest) returns (ProductResponse);
    rpc DeleteProduct(DeleteProductRequest) returns (EmptyResponse);
    rpc GetMyProducts(GetMyProductsRequest) returns (GetProductListResponse);

    // 관리자
    rpc GetSellerList(GetSellerListRequest) returns (GetSellerListResponse);
    rpc ApproveSeller(SellerActionRequest) returns (EmptyResponse);
    rpc RejectSeller(SellerActionRequest) returns (EmptyResponse);
    rpc SuspendSeller(SellerActionRequest) returns (EmptyResponse);
    rpc SuspendProduct(SuspendProductRequest) returns (EmptyResponse);
}

// order_query.proto
service OrderQueryService {
    rpc GetOrderStatus(GetOrderStatusRequest) returns (GetOrderStatusResponse);
    rpc GetOrderList(GetOrderListRequest) returns (GetOrderListResponse);
}

// delivery_query.proto
service DeliveryQueryService {
    rpc GetDeliveryStatus(GetDeliveryStatusRequest) returns (GetDeliveryStatusResponse);
}

// point_query.proto
service PointQueryService {
    rpc GetPointBalance(GetPointBalanceRequest) returns (GetPointBalanceResponse);
}
```

---

## DB 스키마

### bff_db

```sql
users
  id          BIGINT       -- PK
  user_id     VARCHAR(36)  -- UUID
  email       VARCHAR(255)
  name        VARCHAR(100)
  password    VARCHAR(255) -- 소셜 로그인 시 null
  provider    VARCHAR(20)  -- LOCAL, GOOGLE, NAVER
  provider_id VARCHAR(255)

sellers
  id          BIGINT       -- PK
  seller_id   VARCHAR(36)  -- UUID
  email       VARCHAR(255)
  password    VARCHAR(255)
  name        VARCHAR(100)

admins
  id                       BIGINT       -- PK
  admin_id                 VARCHAR(36)  -- UUID
  email                    VARCHAR(255)
  password                 VARCHAR(255)
  name                     VARCHAR(100)
  password_change_required BOOLEAN      -- 임시 비밀번호 변경 필요 여부
  created_by               VARCHAR(36)  -- 생성한 관리자 adminId (최초 관리자는 null)

product_stock
  product_id  VARCHAR(100) -- PK
  quantity    BIGINT       -- 현재 재고 (Redis 스냅샷)
  synced_at   DATETIME
```

### commerce_catalog

```sql
sellers
  id              BIGINT       -- PK
  user_id         VARCHAR(36)
  business_name   VARCHAR(100)
  business_number VARCHAR(20)
  owner_name      VARCHAR(100)
  phone           VARCHAR(20)
  email           VARCHAR(255)
  status          VARCHAR(20)  -- PENDING, APPROVED, REJECTED, SUSPENDED

products
  id          BIGINT       -- PK
  product_id  VARCHAR(36)  -- UUID
  seller_id   BIGINT       -- FK
  name        VARCHAR(200)
  price       DECIMAL(19,2)
  category_id BIGINT
  status      VARCHAR(20)  -- ACTIVE, SUSPENDED

product_images
  id         BIGINT
  product_id BIGINT       -- FK
  url        VARCHAR(500)

product_options
  id         BIGINT
  product_id BIGINT       -- FK
  name       VARCHAR(100)
  extra_price DECIMAL(19,2)
```

### commerce_order

```sql
orders
  order_id      VARCHAR(36)    -- PK
  user_id       VARCHAR(36)
  total_amount  DECIMAL(19,2)
  status        VARCHAR(20)    -- PENDING, CONFIRMED, COMPLETED, CANCELLED

order_items
  order_id      VARCHAR(36)    -- FK
  product_id    VARCHAR(36)
  product_name  VARCHAR(100)
  quantity      INT
  price         DECIMAL(19,2)

outbox_events
  id            VARCHAR(36)    -- PK
  aggregate_id  VARCHAR(36)    -- order_id
  topic         VARCHAR(100)   -- Kafka 토픽
  payload       TEXT           -- JSON 이벤트
  status        VARCHAR(20)    -- PENDING, PUBLISHED, FAILED
```

### commerce_payment

```sql
payments
  payment_id       VARCHAR(36)   -- PK
  order_id         VARCHAR(36)
  amount           DECIMAL(19,2)
  status           VARCHAR(20)   -- PENDING, COMPLETED, FAILED
  toss_payment_key VARCHAR(200)
  method           VARCHAR(50)
```

### commerce_delivery

```sql
deliveries
  delivery_id  VARCHAR(36) -- PK
  order_id     VARCHAR(36)
  user_id      VARCHAR(36)
  address      VARCHAR(255)
  status       VARCHAR(20) -- PENDING, IN_TRANSIT, DELIVERED, CANCELLED
```

### commerce_point

```sql
point_wallets
  user_id      VARCHAR(36) -- PK
  total_point  BIGINT

points
  user_id      VARCHAR(36)
  order_id     VARCHAR(36)
  earned_point BIGINT
  type         VARCHAR(20) -- EARNED, REDEEMED, REFUNDED
```

---

## 실행 방법

### 1. 인프라 실행

```bash
# MySQL, Kafka, Redis, Prometheus, Grafana
docker-compose up -d
```

### 2. application.yml 설정

각 서비스의 `application.yml`에서 아래 항목을 환경에 맞게 수정합니다.

```yaml
# DB 접속 정보
spring.datasource.url
spring.datasource.username
spring.datasource.password

# Redis
spring.data.redis.password

# Kafka
spring.kafka.bootstrap-servers

# 메일 (bff-service)
spring.mail.username
spring.mail.password

# 토스페이먼츠 (payment-service)
toss.payment.secret-key

# JWT (bff-service)
jwt.secret

# OAuth2 (bff-service)
spring.security.oauth2.client.registration.google.client-id
spring.security.oauth2.client.registration.naver.client-id
```

### 3. 서비스 실행 순서

```
1. catalog-service
2. order-service
3. payment-service
4. delivery-service
5. point-service
6. bff-service
```

### 4. Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

---

## 모니터링

```
Prometheus : http://localhost:9090
Grafana    : http://localhost:3000

서킷 브레이커 상태 확인
GET http://localhost:8080/actuator/health
GET http://localhost:8080/actuator/circuitbreakers
```

---

## 이벤트 흐름 예시

### 주문 완료 흐름

```
1. POST /api/orders
   └─ Redis DECR (재고 선점)
   └─ Kafka: order.created 발행

2. order-service: order.created 수신
   └─ 주문 DB 저장
   └─ Kafka: order.confirmed 발행

3. (병렬 처리)
   ├─ payment-service: order.confirmed 수신 → 결제 처리 → payment.completed 발행
   ├─ delivery-service: order.confirmed 수신 → 배송 생성
   └─ point-service: order.confirmed 수신 대기

4. order-service: payment.completed 수신
   └─ 주문 상태 COMPLETED
   └─ Kafka: order.completed 발행

5. (병렬 처리)
   ├─ point-service: order.completed 수신 → 포인트 적립
   └─ bff-service: order.completed 수신 → 완료 메일 발송
```

### 결제 실패 보상 흐름

```
1. payment-service: 결제 실패 → payment.failed 발행

2. order-service: payment.failed 수신
   └─ 주문 상태 CANCELLED
   └─ Outbox: stock.restore 이벤트 저장
   └─ Kafka: order.cancelled 발행

3. bff-service: stock.restore 수신
   └─ Redis INCR (재고 복구)

4. bff-service: order.cancelled 수신
   └─ 취소 메일 발송
```
