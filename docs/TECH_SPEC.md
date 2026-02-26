# 프로젝트 설명서

## 개요

선물하기 플랫폼 API 서버. 사용자가 카테고리와 상품을 관리하고, 위시리스트를 구성하며, 주문을 통해 다른 회원에게 선물을 보낼 수 있다. 카카오 OAuth 로그인과 카카오톡 메시지 전송을 지원한다.

## 기술 스택

| 항목 | 내용 |
|------|------|
| 언어 | Java 21 (Kotlin 지원 포함) |
| 프레임워크 | Spring Boot 3.5.9 |
| 빌드 도구 | Gradle (Kotlin DSL) |
| 데이터베이스 | H2 (개발/테스트), MySQL (운영) |
| 데이터 접근 | Spring Data JPA |
| 마이그레이션 | Flyway |
| 인증 | JWT (JJWT 0.13.0) + 카카오 OAuth2 |
| 템플릿 엔진 | Thymeleaf (Admin UI) |
| 검증 | Bean Validation (spring-boot-starter-validation) |

## 프로젝트 구조 (현재)

```
src/main/java/gift/
├── Application.java                     # Spring Boot 진입점 (@ConfigurationPropertiesScan)
├── auth/                                # 인증 (카카오 OAuth + JWT)
│   ├── AuthenticationResolver.java      # Authorization 헤더 파싱 → Member 조회
│   ├── JwtProvider.java                 # JWT 생성/검증
│   ├── KakaoAuthController.java         # 카카오 로그인 API (/api/auth/kakao)
│   ├── KakaoLoginClient.java            # 카카오 API 호출 (토큰 교환, 유저 정보)
│   ├── KakaoLoginProperties.java        # 카카오 설정 바인딩 (record)
│   └── TokenResponse.java              # JWT 응답 DTO (record)
├── category/                            # 카테고리
│   ├── Category.java                    # 엔티티
│   ├── CategoryController.java          # REST API (/api/categories)
│   ├── CategoryRepository.java          # JpaRepository
│   ├── CategoryRequest.java             # 요청 DTO (record)
│   └── CategoryResponse.java           # 응답 DTO (record)
├── member/                              # 회원
│   ├── AdminMemberController.java       # Admin UI (/admin/members, Thymeleaf)
│   ├── Member.java                      # 엔티티 (포인트, 카카오 토큰 포함)
│   ├── MemberController.java            # REST API (/api/members)
│   ├── MemberRepository.java            # JpaRepository
│   └── MemberRequest.java              # 요청 DTO (record)
├── option/                              # 상품 옵션
│   ├── Option.java                      # 엔티티 (재고 차감 로직 포함)
│   ├── OptionController.java            # REST API (/api/products/{id}/options)
│   ├── OptionNameValidator.java         # 이름 유효성 검증 (static)
│   ├── OptionRepository.java            # JpaRepository
│   ├── OptionRequest.java               # 요청 DTO (record)
│   └── OptionResponse.java             # 응답 DTO (record)
├── order/                               # 주문 (선물 보내기)
│   ├── KakaoMessageClient.java          # 카카오톡 메시지 전송
│   ├── Order.java                       # 엔티티
│   ├── OrderController.java             # REST API (/api/orders)
│   ├── OrderRepository.java             # JpaRepository
│   ├── OrderRequest.java                # 요청 DTO (record)
│   └── OrderResponse.java              # 응답 DTO (record)
├── product/                             # 상품
│   ├── AdminProductController.java      # Admin UI (/admin/products, Thymeleaf)
│   ├── Product.java                     # 엔티티 (Option 1:N cascade)
│   ├── ProductController.java           # REST API (/api/products)
│   ├── ProductNameValidator.java        # 이름 유효성 검증 (static, 카카오 제한)
│   ├── ProductRepository.java           # JpaRepository
│   ├── ProductRequest.java              # 요청 DTO (record)
│   └── ProductResponse.java            # 응답 DTO (record)
└── wish/                                # 위시리스트
    ├── Wish.java                        # 엔티티 (memberId primitive FK)
    ├── WishController.java              # REST API (/api/wishes)
    ├── WishRepository.java              # JpaRepository
    ├── WishRequest.java                 # 요청 DTO (record)
    └── WishResponse.java               # 응답 DTO (record)
```

## 현재 계층 구조의 문제

```
Client → Controller → Repository → DB
              ↑
         비즈니스 로직이 여기에 모두 존재
         (서비스 계층 없음, @Transactional 없음)
```

- **서비스 계층이 없다**: 모든 비즈니스 로직이 컨트롤러에 직접 구현되어 있다.
- **트랜잭션 경계가 없다**: 여러 엔티티를 수정하는 `OrderController.createOrder`에도 `@Transactional`이 없다.
- **로직 중복**: `MemberController`와 `AdminMemberController`, `ProductController`와 `AdminProductController`가 동일한 로직을 각각 구현한다.
- **인증 패턴 반복**: `WishController`, `OrderController`에서 동일한 인증 체크 코드가 복붙되어 있다.

## 목표 계층 구조 (리팩터링 후)

```
Client → Controller → Service → Repository → DB
              ↑            ↑
         요청 검증,     비즈니스 로직,
         위임만 담당    @Transactional
```

## 도메인 모델

| 엔티티 | 필드 | 관계 | 비즈니스 로직 |
|--------|------|------|---------------|
| Category | id, name, color, imageUrl, description | Product(1:N) | update() |
| Product | id, name, price, imageUrl | Category(N:1), Option(1:N cascade) | update() |
| Option | id, name, quantity | Product(N:1) | subtractQuantity() — 재고 부족 시 예외 |
| Member | id, email, password, kakaoAccessToken, point | - | chargePoint(), deductPoint(), update() |
| Wish | id, memberId(primitive FK) | Product(N:1) | - |
| Order | id, memberId(primitive FK), quantity, message, orderDateTime | Option(N:1) | - |

### 엔티티 관계도

```
Category (1) ──────< Product (N)
                        │
                   (1) ──────< Option (N)
                                   │
                              (N:1) Option ──── Order (N)

Member ──── (primitive FK) ──── Wish ──── @ManyToOne ──── Product
Member ──── (primitive FK) ──── Order
```

## API 엔드포인트

### 인증

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/auth/kakao/login` | 카카오 OAuth 로그인 리다이렉트 | 불필요 |
| GET | `/api/auth/kakao/callback` | 카카오 OAuth 콜백 (토큰 교환 → JWT 발급) | 불필요 |
| POST | `/api/members/register` | 회원가입 (email, password → JWT) | 불필요 |
| POST | `/api/members/login` | 로그인 (email, password → JWT) | 불필요 |

### 카테고리

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/categories` | 전체 조회 | 불필요 |
| POST | `/api/categories` | 생성 | 불필요 |
| PUT | `/api/categories/{id}` | 수정 | 불필요 |
| DELETE | `/api/categories/{id}` | 삭제 | 불필요 |

### 상품

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/products` | 전체 조회 (페이징) | 불필요 |
| GET | `/api/products/{id}` | 단건 조회 | 불필요 |
| POST | `/api/products` | 등록 (이름 검증, 카카오 제한) | 불필요 |
| PUT | `/api/products/{id}` | 수정 | 불필요 |
| DELETE | `/api/products/{id}` | 삭제 | 불필요 |

### 옵션

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/products/{productId}/options` | 상품별 옵션 조회 | 불필요 |
| POST | `/api/products/{productId}/options` | 옵션 추가 (이름 검증, 중복 불가) | 불필요 |
| DELETE | `/api/products/{productId}/options/{optionId}` | 옵션 삭제 (최소 1개 유지) | 불필요 |

### 위시리스트

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/wishes` | 내 위시리스트 조회 (페이징) | JWT 필수 |
| POST | `/api/wishes` | 위시리스트 추가 (중복 시 기존 반환) | JWT 필수 |
| DELETE | `/api/wishes/{id}` | 위시리스트 삭제 (소유권 검증) | JWT 필수 |

### 주문

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/orders` | 내 주문 목록 조회 (페이징) | JWT 필수 |
| POST | `/api/orders` | 주문 생성 (재고 차감 → 포인트 차감 → 저장 → 카카오톡 알림) | JWT 필수 |

### Admin (Thymeleaf)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET/POST | `/admin/members/**` | 회원 관리 (목록, 생성, 수정, 포인트 충전, 삭제) |
| GET/POST | `/admin/products/**` | 상품 관리 (목록, 생성, 수정, 삭제) |

## 주요 설정

```properties
spring.application.name=spring-gift
jwt.secret=${JWT_SECRET:a-string-secret-at-least-256-bits-long}
jwt.expiration=${JWT_EXPIRATION:3600000}
kakao.login.client-id=${KAKAO_CLIENT_ID:}
kakao.login.client-secret=${KAKAO_CLIENT_SECRET:}
kakao.login.redirect-uri=${KAKAO_REDIRECT_URI:http://localhost:8080/api/auth/kakao/callback}
```

- DB: Flyway 마이그레이션 (V1: 스키마, V2: 시드 데이터)
- 카카오 OAuth/메시지 API 키는 환경 변수로 주입

## 빌드 및 실행

```bash
# 빌드
./gradlew clean build -x test

# 실행
./gradlew bootRun

# 테스트
./gradlew test
```

## 관련 문서

- [FEATURES.md](FEATURES.md) — 핵심 기능 명세서
- [TEST_STRATEGY.md](TEST_STRATEGY.md) — 테스트 전략 문서
