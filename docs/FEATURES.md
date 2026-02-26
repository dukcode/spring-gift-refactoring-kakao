# 핵심 기능 명세서

## 프로젝트 개요

선물하기 플랫폼 API 서버로, 사용자가 카테고리와 상품을 관리하고, 위시리스트를 구성하며, 주문을 통해 다른 회원에게 선물을 보낼 수 있는 기능을 제공한다. 카카오 OAuth 로그인과 카카오톡 메시지 전송을 지원한다.

- **프레임워크:** Spring Boot 3.5.9
- **언어:** Java 21
- **데이터베이스:** H2 (개발/테스트), MySQL (운영)
- **데이터 접근:** Spring Data JPA + Flyway

---

## 기능 목록

### 1. 회원가입

사용자가 이메일과 비밀번호로 회원가입한다. 가입 즉시 JWT를 발급받는다.

| 항목 | 내용 |
|------|------|
| **API** | `POST /api/members/register` |
| **요청 본문** | `{ "email": "string", "password": "string" }` |
| **응답** | `201` + `TokenResponse { token }` |
| **규칙** | 이메일 중복 시 `IllegalArgumentException` (400) |

---

### 2. 로그인

등록된 이메일과 비밀번호로 로그인하여 JWT를 발급받는다.

| 항목 | 내용 |
|------|------|
| **API** | `POST /api/members/login` |
| **요청 본문** | `{ "email": "string", "password": "string" }` |
| **응답** | `200` + `TokenResponse { token }` |
| **규칙** | 이메일 미존재 또는 비밀번호 불일치 시 `IllegalArgumentException` (400) |

---

### 3. 카카오 로그인

카카오 OAuth2 인증 코드를 통해 로그인하거나 자동 회원가입한다.

| 항목 | 내용 |
|------|------|
| **API (리다이렉트)** | `GET /api/auth/kakao/login` → 카카오 인증 페이지로 302 |
| **API (콜백)** | `GET /api/auth/kakao/callback?code=` |
| **응답** | `TokenResponse { token }` |

**처리 흐름:**

1. 인증 코드로 카카오 액세스 토큰 교환
2. 액세스 토큰으로 카카오 유저 정보(이메일) 조회
3. 이메일로 기존 회원 검색 또는 신규 회원 생성
4. 카카오 액세스 토큰을 회원에 저장
5. JWT 발급하여 반환

---

### 4. 카테고리 관리

상품을 분류할 수 있는 카테고리를 CRUD한다.

| 항목 | 내용 |
|------|------|
| **API** | `GET/POST /api/categories`, `PUT/DELETE /api/categories/{id}` |
| **요청 본문** | `{ "name": "string", "color": "#HEX", "imageUrl": "string", "description": "string" }` |
| **응답** | `CategoryResponse { id, name, color, imageUrl, description }` |

---

### 5. 상품 관리

카테고리에 속하는 상품을 등록/조회/수정/삭제한다.

| 항목 | 내용 |
|------|------|
| **API** | `GET /api/products` (페이징), `GET/POST /api/products/{id}`, `PUT/DELETE /api/products/{id}` |
| **요청 본문** | `{ "name": "string", "price": int, "imageUrl": "string", "categoryId": long }` |
| **응답** | `ProductResponse { id, name, price, imageUrl, categoryId }` |
| **규칙** | 이름: 최대 15자, 허용 문자(한글/영문/숫자/특수), "카카오" 포함 불가 (API). 존재하지 않는 카테고리 시 404. |

---

### 6. 상품 옵션 관리

특정 상품에 대한 옵션(변형)을 관리한다. 옵션에는 이름과 재고 수량이 포함된다.

| 항목 | 내용 |
|------|------|
| **API** | `GET/POST /api/products/{productId}/options`, `DELETE .../options/{optionId}` |
| **요청 본문** | `{ "name": "string", "quantity": int }` |
| **응답** | `OptionResponse { id, name, quantity }` |
| **규칙** | 이름: 최대 50자, 동일 상품 내 중복 불가. 마지막 옵션은 삭제 불가. |

---

### 7. 위시리스트 관리

인증된 사용자가 원하는 상품을 위시리스트에 추가/조회/삭제한다.

| 항목 | 내용 |
|------|------|
| **API** | `GET/POST /api/wishes`, `DELETE /api/wishes/{id}` |
| **인증** | `Authorization: Bearer <JWT>` 필수 |
| **요청 본문** | `{ "productId": long }` |
| **응답** | `WishResponse { id, productId, name, price, imageUrl }` |
| **규칙** | 중복 추가 시 기존 위시 반환 (200). 삭제 시 소유권 검증 (403). |

---

### 8. 주문 (선물 보내기)

인증된 사용자가 옵션을 선택하여 주문한다. 핵심 비즈니스 기능으로, 재고 차감, 포인트 결제, 카카오톡 알림이 포함된다.

| 항목 | 내용 |
|------|------|
| **API** | `POST /api/orders` |
| **인증** | `Authorization: Bearer <JWT>` 필수 |
| **요청 본문** | `{ "optionId": long, "quantity": int, "message": "string" }` |
| **응답** | `201` + `OrderResponse { id, optionId, quantity, orderDateTime, message }` |

**처리 흐름:**

1. 인증 확인 (401)
2. 옵션 조회 (404)
3. 재고 차감 — `option.subtractQuantity(quantity)`, 부족 시 `IllegalArgumentException`
4. 포인트 차감 — `member.deductPoint(price * quantity)`, 부족 시 `IllegalArgumentException`
5. 주문 저장
6. (미구현) 위시리스트에서 해당 상품 자동 제거
7. 카카오톡 메시지 전송 (best-effort, 실패 무시)

---

### 9. 주문 목록 조회

인증된 사용자의 주문 내역을 페이징으로 조회한다.

| 항목 | 내용 |
|------|------|
| **API** | `GET /api/orders` |
| **인증** | `Authorization: Bearer <JWT>` 필수 |
| **응답** | `Page<OrderResponse>` |

---

### 10. 재고 자동 관리

주문 시 선택된 옵션의 재고 수량이 자동으로 차감된다.

| 항목 | 내용 |
|------|------|
| **트리거** | 주문 API 호출 시 |
| **동작** | `Option.subtractQuantity(quantity)` 호출 |
| **예외** | 요청 수량 > 잔여 수량일 때 `IllegalArgumentException` |

---

### 11. 포인트 시스템

회원에게 포인트를 충전하고, 주문 시 포인트로 결제한다.

| 항목 | 내용 |
|------|------|
| **충전** | Admin UI에서 `POST /admin/members/{id}/charge-point` |
| **차감** | 주문 시 `member.deductPoint(상품가격 * 수량)` |
| **규칙** | 충전/차감 금액 ≤ 0이면 예외. 잔액 부족 시 예외. |

---

### 12. Admin 회원/상품 관리

Thymeleaf 기반 관리자 웹 UI로 회원과 상품을 관리한다.

| 항목 | 내용 |
|------|------|
| **회원** | `/admin/members` — 목록, 생성, 수정, 포인트 충전, 삭제 |
| **상품** | `/admin/products` — 목록, 생성, 수정, 삭제 (카카오 이름 제한 없음) |

---

## 외부 연동

### 카카오 OAuth2

| 항목 | 내용 |
|------|------|
| **인증 URL** | `kauth.kakao.com/oauth/authorize` |
| **토큰 교환** | `kauth.kakao.com/oauth/token` |
| **유저 정보** | `kapi.kakao.com/v2/user/me` |
| **설정** | `KakaoLoginProperties` (clientId, clientSecret, redirectUri) |

### 카카오톡 메시지

| 항목 | 내용 |
|------|------|
| **메시지 API** | `kapi.kakao.com/v2/api/talk/memo/default/send` |
| **클라이언트** | `KakaoMessageClient` |
| **트리거** | 주문 생성 시, 회원에게 카카오 액세스 토큰이 있으면 전송 |

---

## 아키텍처 (현재 — 리팩터링 전)

```
Client Request
    │
    ▼
Controller (직접 Repository 호출, 비즈니스 로직 포함)
    │
    ▼
Repository
    │
    ▼
Database (H2 / MySQL)
```

- 서비스 계층 없음
- `@Transactional` 없음
- 테스트 코드 없음
