# spring-gift-refactoring

## 프로젝트 소개

Spring Boot 기반의 선물하기 서비스. 사용자가 카테고리와 상품을 관리하고, 위시리스트를 구성하며, 주문을 통해 다른 회원에게 선물을 보낼 수 있다. 카카오 OAuth 로그인과 카카오톡 메시지 알림을 지원한다.

| 항목 | 내용 |
|------|------|
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 3.5.9 |
| 데이터베이스 | H2 (개발/테스트), MySQL (운영) |
| 인증 | JWT + 카카오 OAuth2 |
| 빌드 | Gradle (Kotlin DSL), Flyway 마이그레이션 |

---

## 구현 기능 목록

### 인증

- [x] 이메일/비밀번호 회원가입 (`POST /api/members/register`) — JWT 즉시 발급
- [x] 이메일/비밀번호 로그인 (`POST /api/members/login`) — JWT 발급
- [x] 카카오 OAuth 로그인 (`GET /api/auth/kakao/login` → 콜백 → JWT 발급)
- [x] 카카오 로그인 시 신규 회원 자동 등록 및 액세스 토큰 저장

### 카테고리

- [x] 카테고리 목록 조회 (`GET /api/categories`)
- [x] 카테고리 생성 (`POST /api/categories`)
- [x] 카테고리 수정 (`PUT /api/categories/{id}`)
- [x] 카테고리 삭제 (`DELETE /api/categories/{id}`)

### 상품

- [x] 상품 목록 조회 - 페이징 (`GET /api/products`)
- [x] 상품 단건 조회 (`GET /api/products/{id}`)
- [x] 상품 등록 (`POST /api/products`) — 이름 검증 (최대 15자, "카카오" 포함 불가)
- [x] 상품 수정 (`PUT /api/products/{id}`)
- [x] 상품 삭제 (`DELETE /api/products/{id}`)

### 옵션

- [x] 상품별 옵션 조회 (`GET /api/products/{productId}/options`)
- [x] 옵션 추가 (`POST /api/products/{productId}/options`) — 이름 중복 불가, 최대 50자
- [x] 옵션 삭제 (`DELETE .../options/{optionId}`) — 마지막 1개는 삭제 불가

### 위시리스트

- [x] 내 위시리스트 조회 - 페이징 (`GET /api/wishes`) — JWT 인증 필수
- [x] 위시리스트 추가 (`POST /api/wishes`) — 중복 시 기존 반환 (멱등)
- [x] 위시리스트 삭제 (`DELETE /api/wishes/{id}`) — 소유권 검증

### 주문 (선물 보내기)

- [x] 주문 생성 (`POST /api/orders`) — 재고 차감 → 포인트 차감 → 저장 → 카카오톡 알림
- [x] 내 주문 목록 조회 - 페이징 (`GET /api/orders`) — JWT 인증 필수

### 포인트

- [x] Admin에서 포인트 충전 (`POST /admin/members/{id}/charge-point`)
- [x] 주문 시 포인트 자동 차감 (상품 가격 * 수량)

### Admin (Thymeleaf)

- [x] 회원 관리 — 목록, 생성, 수정, 포인트 충전, 삭제 (`/admin/members`)
- [x] 상품 관리 — 목록, 생성, 수정, 삭제 (`/admin/products`) — 카카오 이름 제한 없음

---

## 리팩터링 계획 (1단계 목표)

> 목표: **작동을 바꾸지 않고** 변경하기 쉬운 구조를 만든다.

### 스타일 정리

- [x] Google Java Style 기반으로 코드 포맷 통일
- [x] 불필요한 공백, 빈 줄, 일관성 없는 들여쓰기 정리
- [x] import 순서 정리 (미사용 import 제거)

### 불필요한 코드(Dead Code) 제거

- [x] IDE 정적 분석으로 미사용 필드/메서드/변수 식별
- [x] `OrderController`에 주입되었으나 사용되지 않는 `WishRepository` 제거
- [x] 삭제 전 git blame으로 의도 확인, TODO/주석에 이유가 있는지 확인

### 서비스 계층 추출 (구조 변경, 작동 변경 없음)

> Controller의 비즈니스 로직을 Service로 이동. Controller는 요청 검증과 위임만 담당.

- [x] `OrderService` 추출 — 최우선. 7단계 주문 흐름(재고 차감 + 포인트 차감 + 저장 + 카카오 알림)을 `@Transactional` 서비스로 이동
- [x] `MemberService` 추출 — `MemberController` + `AdminMemberController`의 중복 로직 통합
- [x] `ProductService` 추출 — `ProductController` + `AdminProductController`의 중복 로직 통합
- [x] `OptionService` 추출 — 이름 검증, 중복 체크, 최소 1개 규칙을 서비스로 이동
- [x] `WishService` 추출 — 중복 추가 방지, 소유권 검증을 서비스로 이동
- [x] `CategoryService` 추출 — 단순 CRUD이지만 계층 일관성을 위해 추출
- [x] `KakaoAuthService` 추출 — OAuth 콜백 흐름(토큰 교환 → 회원 조회/생성 → JWT 발급)을 서비스로 이동

---

## 구현 전략

### 전체 원칙

1단계의 핵심은 **"구조만 바꾸고 작동은 바꾸지 않는다"**이다. 신규 기능을 추가하지 않으며, 기존 API의 요청/응답이 달라지지 않아야 한다.

### 진행 순서

작업은 안전한 변경부터 위험한 변경 순서로 진행한다. 각 단계를 완료한 뒤 다음으로 넘어간다.

```
1. 테스트 작성 (안전망 확보)
   ↓
2. 스타일 정리 (가장 안전한 변경)
   ↓
3. Dead Code 제거 (작은 범위 삭제)
   ↓
4. 서비스 추출 (가장 큰 구조 변경)
```

### 1. 테스트 작성 — 리팩터링의 안전망 확보

현재 테스트가 0개이므로, **구조를 바꾸기 전에 현재 작동을 고정하는 테스트를 먼저 작성**한다. 테스트 없이 리팩터링하면 작동이 바뀌어도 알 수 없다.

- `@SpringBootTest(RANDOM_PORT)` + RestAssured로 API 통합 테스트 작성
- `@Sql` 기반으로 테스트 데이터를 준비하고 정리
- 주문 생성(재고/포인트 차감)을 최우선으로 테스트
- 테스트는 **현재 작동을 그대로 캡처**한다 (버그가 있어도 현재 동작 기준으로 작성)

### 2. 스타일 정리 — 가장 안전한 변경부터

- 포매터/린터를 사용하여 기계적으로 처리
- 수동 작업은 최소화 (사람이 판단해야 하는 변경은 뒤로 미룬다)
- 변경 후 전체 테스트 실행하여 작동 유지 확인
- **하나의 커밋**으로 묶는다 (`style` 타입)

### 3. Dead Code 제거 — 근거 확인 후 삭제

- IDE 정적 분석(IntelliJ Inspect)으로 미사용 항목을 기계적으로 식별
- 삭제 전 체크리스트:
  - 주변 주석/TODO에 의도가 있는가?
  - git blame으로 누가 왜 추가했는가?
  - 이후 단계(서비스 추출)에서 필요한 코드가 아닌가?
- 확인된 Dead Code만 제거, 변경 후 전체 테스트 실행
- **하나의 커밋**으로 묶는다 (`chore` 타입)

### 4. 서비스 계층 추출 — 한 번에 하나씩

서비스 추출은 **가장 위험한 구조 변경**이므로, 한 번에 하나의 서비스만 추출하고 그때마다 테스트를 돌린다.

**추출 순서** (의존성이 적은 것 → 복잡한 것):

| 순서 | 서비스 | 이유 |
|------|--------|------|
| 1 | `CategoryService` | 가장 단순. 다른 서비스에 의존 없음. 패턴 연습용. |
| 2 | `MemberService` | 두 컨트롤러의 중복 제거. 이후 서비스들이 의존. |
| 3 | `ProductService` | 두 컨트롤러의 중복 제거. Option이 의존. |
| 4 | `OptionService` | Product 의존. 비즈니스 규칙(중복, 최소 1개) 포함. |
| 5 | `WishService` | 인증 + 소유권 검증 로직 이동. |
| 6 | `KakaoAuthService` | 외부 API 연동 흐름 분리. |
| 7 | `OrderService` | 가장 복잡. 여러 서비스/엔티티에 걸친 트랜잭션. 마지막에 추출. |

**각 서비스 추출 절차**:

```
1. 컨트롤러에서 비즈니스 로직 식별
2. Service 클래스 생성, @Service + @Transactional 적용
3. 로직을 Service로 이동 (복사 → 컨트롤러에서 호출 변경 → 원본 삭제)
4. 컨트롤러는 요청 검증 + Service 위임만 남김
5. 전체 테스트 실행하여 작동 유지 확인
6. 커밋 (refactor 타입, 서비스 1개 단위)
```

### 검증 기준

모든 단계에서 아래를 만족해야 다음으로 넘어간다:

- `./gradlew test` 전체 통과
- `git diff`로 의도한 변경만 포함되어 있는지 확인
- 새로운 API 엔드포인트가 추가되지 않았는지 확인
- 기존 API의 요청/응답 형식이 바뀌지 않았는지 확인

---

## 커밋 규칙

[AngularJS Git Commit Message Conventions](https://docs.google.com/document/d/1QrDFcIiPjSLDn3EL15IJygNPiHORgU1_OOAqWjiDU5Y/edit) 준수

---

## AI 활용 기록

AI 도구(Claude Code)를 활용한 과정과 의사결정은 아래 문서에 기록한다.

- [`chatlog/AI_USAGE_STEP_1.md`](chatlog/AI_USAGE_STEP_1.md) — 리팩터링 준비 단계 AI 사용 기록
