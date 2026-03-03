# STEP 1: 리팩터링 준비 - AI 사용 기록

## 개요

이 문서는 리팩터링 준비 단계에서 AI를 어떻게 활용했는지를 기록한다.

AI는 크게 두 가지 역할로 활용되었다.

1. **기존 코드베이스 전체 분석** — 도메인, 계층 구조, 문제점 파악
2. **문서 체계 구축** — 리팩터링을 위한 기준 문서(TECH_SPEC, FEATURES, TEST_STRATEGY, CLAUDE.md) 작성

---

## 1단계: 두 프로젝트 전체 분석

### 프롬프트

> 이전 미션(spring-gift-test-kakao)과 새 미션(spring-gift-refactoring-kakao) 두 프로젝트의 모든 도메인을 이해해봐. 특히 기존 미션에서 chatlog 남기는 방식이나 CLAUDE.md 같은 거 눈여겨봐.

### AI 활용 방식

- 3개의 병렬 탐색 에이전트를 실행:
  1. spring-gift-test-kakao 전체 구조 분석 (82개 파일 탐색)
  2. spring-gift-refactoring-kakao 전체 구조 분석 (67개 파일 탐색)
  3. chatlog, CLAUDE.md, README.md 등 문서 체계 분석 (23개 파일 탐색)
- 추가로 리팩터링 대상 컨트롤러 전체 소스 코드를 읽고 서비스 추출 대상 분석

### 분석 결과

#### 이전 미션 (spring-gift-test-kakao)

- 잘 구조화된 3계층 아키텍처 (Controller → Service → Repository)
- 6개 도메인: Category, Product, Option, Member, Wish, Gift(값객체)
- 테스트 13개 (JUnit5 RestAssured 6 + Cucumber BDD 7)
- Docker 기반 이중 테스트 인프라 (H2 단위 + PostgreSQL 인수)
- AI 기록 체계: `CLAUDE.md` + `chatlog/AI_USAGE_STEP_N.md` + `docs/`

#### 새 미션 (spring-gift-refactoring-kakao)

- **서비스 계층이 전혀 없음** — 모든 비즈니스 로직이 컨트롤러에 직접 구현
- 6개 엔티티: Category, Product, Option, Member, Wish, Order
- 추가 기능: 카카오 OAuth, JWT 인증, 포인트 시스템, 카카오톡 메시지, Admin UI
- **테스트 코드 0개** — 빈 디렉토리
- Flyway 마이그레이션 사용 (MySQL/H2)

#### 핵심 차이점

| 항목 | 이전 미션 | 새 미션 |
|------|-----------|---------|
| 서비스 계층 | 있음 | **없음** |
| @Transactional | 모든 Service에 적용 | **없음** |
| 테스트 | 13개 | **0개** |
| 인증 | 없음 (Member-Id 헤더) | JWT + 카카오 OAuth |
| 결제 | 없음 | 포인트 시스템 |

---

## 2단계: 리팩터링 대상 식별

### AI 활용 방식

- 모든 컨트롤러 소스 코드를 직접 읽고 서비스 추출 대상과 우선순위를 분석

### 분석 결과 — 서비스 추출 대상

| 우선순위 | 컨트롤러 | 문제 |
|----------|----------|------|
| **최고** | `OrderController` | 7단계 트랜잭션이 `@Transactional` 없이 컨트롤러에 존재. 재고차감 + 포인트차감 + 주문저장이 원자적이지 않음. 위시 클린업 미구현. |
| **높음** | `MemberController` + `AdminMemberController` | 회원 등록/수정 로직이 두 컨트롤러에 중복 |
| **높음** | `ProductController` + `AdminProductController` | 상품 CRUD 로직이 두 컨트롤러에 중복 |
| **중간** | `WishController` | 인증 + 소유권 체크 + CRUD가 컨트롤러에 혼재 |
| **중간** | `OptionController` | 이름 검증 + 중복 체크 + 최소 1개 규칙이 컨트롤러에 |
| **중간** | `KakaoAuthController` | OAuth 콜백 전체 흐름(토큰 교환 → 회원 조회/생성 → JWT 발급)이 컨트롤러에 |
| **낮음** | `CategoryController` | 상대적으로 단순한 CRUD |

### 발견된 이슈

1. **트랜잭션 부재**: `OrderController.createOrder`에서 `option.subtractQuantity()` 후 `member.deductPoint()`가 실패하면 재고만 차감되고 포인트는 유지되는 데이터 불일치 발생 가능
2. **위시 클린업 미구현**: 코드 주석에 "step 6: cleanup wish"가 있으나 실제 구현은 누락. `wishRepository`가 주입되어 있으나 사용되지 않음
3. **인증 패턴 반복**: `WishController`, `OrderController`에서 동일한 인증 체크 코드가 복붙

---

## 3단계: 이전 미션의 AI 기록 컨벤션 분석

### AI 활용 방식

- `CLAUDE.md`, `chatlog/AI_USAGE_STEP_1.md`, `chatlog/AI_USAGE_STEP_2.md` 전문을 읽고 패턴 추출

### 분석 결과 — 컨벤션 정리

| 컨벤션 | 상세 |
|--------|------|
| AI 지시 파일 | 프로젝트 루트에 `CLAUDE.md` (컨텍스트 + 명령어 + 워크플로우 규칙) |
| AI 기록 디렉토리 | `chatlog/` 루트에 배치 |
| 기록 파일명 | `AI_USAGE_STEP_N.md` (단계별 1파일) |
| 기록 형식 | `**Prompt**`, `**Action**`, `**Outcome**` 3필드 (선택: `**교훈**`, `**참고**`) |
| 지식 베이스 | `docs/` 디렉토리에 FEATURES.md, TECH_SPEC.md, TEST_STRATEGY.md |

### AI 활용 패턴 (이전 미션에서 추출)

1. **구조 먼저 설계** — 빈 템플릿을 만들고 섹션별로 채워가기
2. **반복적 제안-피드백** — AI 방안 제시 → 사용자 제약 추가 → AI 재제안
3. **코드 생성 후 실행 검증** — 전략 문서 기반 코드 생성 → 실행 → 실패 분석

---

## 4단계: 새 프로젝트 문서 체계 구축

### 프롬프트

> 기존 프로젝트에 있는 docs/ 내에 있는 파일들, CLAUDE.md 파일들을 새 프로젝트 도메인에 맞게 수정해서 다 작성해줘. chatlog/AI_USAGE_STEP_1에 의사결정 과정 기록해줘.

### AI 활용 방식

- 이전 미션의 문서 구조를 템플릿으로 삼되, 새 프로젝트의 도메인(주문, 포인트, 카카오 OAuth 등)에 맞게 내용을 재작성
- 현재 코드의 문제점(서비스 계층 부재, 트랜잭션 부재)을 문서에 명시

### 산출물

| 파일 | 내용 |
|------|------|
| `CLAUDE.md` | 프로젝트 컨텍스트, 명령어, 리팩터링 워크플로우 규칙 |
| `docs/TECH_SPEC.md` | 현재 구조 + 목표 구조 + 도메인 모델 + 전체 API 명세 |
| `docs/FEATURES.md` | 12개 기능 명세 (인증, CRUD, 주문, 포인트, Admin, 외부 연동) |
| `docs/TEST_STRATEGY.md` | 테스트 목적, 우선순위, 데이터 전략, 검증 전략 |
| `chatlog/AI_USAGE_STEP_1.md` | 본 문서 (AI 활용 기록) |

---

## 5단계: README.md 작업 지시서 작성

### 프롬프트

> 분석 내용을 바탕으로 README.md를 작성해줘. 프로젝트 소개, 구현 기능 목록, 리팩터링 계획(1단계 목표), 구현 전략, 커밋 규칙을 포함해서.

### AI 활용 방식

- 코드 분석 결과를 기반으로 이미 구현된 기능 목록을 체크리스트로 정리
- 리팩터링 계획을 3단계(스타일 정리 → Dead Code 제거 → 서비스 추출)로 구조화
- 구현 전략에 진행 순서와 서비스 추출 순서(의존성 적은 것 → 복잡한 것)를 명시

### 산출물

| 파일 | 내용 |
|------|------|
| `README.md` | 프로젝트 소개, 구현 기능 목록(체크리스트), 리팩터링 계획, 구현 전략(순서/절차/검증기준), 커밋 규칙, AI 활용 기록 링크 |

---

## 6단계: Cucumber BDD 의존성 추가 및 Feature 파일 작성

- **Prompt**: Cucumber 의존성을 build.gradle.kts에 추가하고, 주문(선물 보내기) 시나리오를 Gherkin 한글 문법으로 gift.feature 파일로 작성
- **Action**:
  - `build.gradle.kts`: cucumber-java, cucumber-spring, cucumber-junit-platform-engine, junit-platform-suite, rest-assured 의존성 추가. `test` 태스크에서 cucumber 엔진 제외, `cucumberTest` 태스크 등록
  - `src/test/resources/features/gift.feature`: 배경(회원, 카테고리, 상품, 옵션) + 주문 성공 시나리오 1개 작성
  - `src/test/resources/junit-platform.properties`: Cucumber 엔진 설정 (features 경로, glue 패키지)
- **Outcome**: `./gradlew clean build -x test` BUILD SUCCESSFUL. ktlint이 build.gradle.kts의 체인 메서드 줄바꿈을 요구하여 2회 수정 후 통과.

---

## 7단계: Step Definitions 및 Cucumber 설정 구현

- **Prompt**: gift.feature를 실행할 수 있도록 GiftStepDefinitions, CommonStepDefinitions, CucumberSpringConfiguration, ScenarioContext 작성. `./gradlew cucumberTest`로 검증.
- **Action**:
  - `src/test/resources/application.properties`: H2(MySQL 호환 모드) + Flyway 비활성화 + JWT/카카오 테스트 설정
  - `CucumberSpringConfiguration.java`: `@CucumberContextConfiguration` + `@SpringBootTest(RANDOM_PORT)`
  - `ScenarioContext.java`: `@Component @ScenarioScope` — Response, ids(이름→ID), tokens(이름→JWT) 공유
  - `CommonStepDefinitions.java`: `@Before` DB 초기화(TRUNCATE) + 회원 Given(JdbcTemplate + KeyHolder + JwtProvider로 토큰 생성) + 카테고리/상품 Given + 응답코드 Then
  - `GiftStepDefinitions.java`: 옵션 Given + 주문 When(JWT Authorization 헤더 포함)
- **Outcome**: 첫 실행 시 `PlaceholderResolutionException` 발생 — 테스트용 `application.properties`가 main 것을 덮어써서 JWT 설정이 누락됨. JWT/카카오 프로퍼티 추가 후 `./gradlew cucumberTest` BUILD SUCCESSFUL (1 시나리오 통과).
- **교훈**: `src/test/resources/application.properties`는 main의 동명 파일을 완전히 덮어쓰므로, 테스트에서도 필요한 모든 프로퍼티를 명시해야 한다.

---

## 8단계: gift.feature 시나리오 확장 (재고/포인트 예외 케이스)

- **Prompt**: 나머지 테스트 케이스들(재고 부족, 예외 케이스 등)도 모두 gift.feature에 시나리오로 추가하고 검증
- **Action**:
  - `gift.feature`: 1개 → 6개 시나리오로 확장
    1. 재고와 포인트가 충분할 때 주문 성공 (201)
    2. 주문 후 재고가 정확히 차감된다 (3+7=201, 1더→500)
    3. 재고 전량 주문 후 추가 주문 시도 실패 (10→201, 1더→500)
    4. 재고 부족 시 주문 실패하고 재고는 유지된다 (11→500, 10→201)
    5. 재고가 0일 때 주문 실패 (품절 옵션, 1→500)
    6. 포인트 부족 시 주문 실패 (100포인트로 25000원 상품→500)
  - Step Definition 수정 불필요 — 기존 패턴으로 모든 시나리오 커버
- **Outcome**: `./gradlew cucumberTest` BUILD SUCCESSFUL (6 시나리오 통과).
- **참고**: "포인트 부족 후 재고 미차감 증명"은 의도적으로 제외. 현재 `@Transactional` 부재로 재고 차감 후 포인트 실패 시 재고만 빠지는 버그가 있어, 서비스 추출 + `@Transactional` 적용 후 보완 예정.

---

## 9단계: product.feature 추가 (상품 등록 시나리오)

- **Prompt**: product.feature도 추가해줘
- **Action**:
  - `src/test/resources/features/product.feature`: 3개 시나리오 작성
    1. 상품 등록 성공 (201 + 상품 수 확인 + 카테고리 검증)
    2. 존재하지 않는 카테고리로 등록 시 실패 (404 + 0개)
    3. "카카오" 포함 이름으로 등록 시 실패 (400 + 0개)
  - `ProductStepDefinitions.java`: 상품 등록 When 2개(정상 카테고리, 존재하지 않는 카테고리) + 상품 수 확인 Then + 카테고리 검증 Then
- **Outcome**: `./gradlew cucumberTest` BUILD SUCCESSFUL (9 시나리오: gift 6 + product 3 전체 통과).

---

## 10단계: option.feature 추가 (옵션 관리 시나리오)

- **Prompt**: 나머지 cucumberTest들도 작성해줘. 단, 기존 프로덕션 코드는 수정하면 안돼.
- **Action**:
  - `src/test/resources/features/option.feature`: 5개 시나리오 작성
    1. 옵션 추가 성공 (201 + 옵션 수 2개 확인)
    2. 중복 옵션명 추가 시 실패 (400)
    3. 옵션이 2개일 때 삭제 성공 (204 + 옵션 수 1개 확인)
    4. 옵션이 1개일 때 삭제 시 실패 (400 + 옵션 수 1개 유지)
    5. 존재하지 않는 상품에 옵션 추가 시 실패 (404)
  - `OptionStepDefinitions.java`: 옵션 추가 When(POST), 존재하지 않는 상품에 추가 When, 옵션 삭제 When(DELETE), 옵션 개수 확인 Then(GET)
- **Outcome**: `./gradlew cucumberTest` BUILD SUCCESSFUL (14 시나리오: gift 6 + product 3 + option 5 전체 통과).

---

## 11단계: wish.feature 추가 (위시리스트 관리 시나리오)

- **Prompt**: wish도 가보자
- **Action**:
  - `src/test/resources/features/wish.feature`: 6개 시나리오 작성
    1. 위시리스트에 상품 추가 성공 (201)
    2. 이미 추가된 상품을 다시 추가하면 기존 위시 반환 (201→200)
    3. 자신의 위시 삭제 성공 (204)
    4. 다른 사용자의 위시 삭제 시 실패 (403)
    5. 존재하지 않는 상품을 위시리스트에 추가 시 실패 (404)
    6. 잘못된 인증으로 위시리스트 조회 시 실패 (401)
  - `WishStepDefinitions.java`: 위시 추가 When(POST, 응답에서 wishId 추출→ScenarioContext 저장), 마지막 위시 삭제 When(DELETE), 존재하지 않는 상품 추가 When, 잘못된 인증 조회 When
- **Outcome**: `./gradlew cucumberTest` BUILD SUCCESSFUL (20 시나리오: gift 6 + product 3 + option 5 + wish 6 전체 통과).

---

## 12단계: member.feature 추가 (회원 관리 시나리오)

- **Prompt**: member도 가보자
- **Action**:
  - `src/test/resources/features/member.feature`: 5개 시나리오 작성
    1. 회원 가입 성공 (201 + 토큰 포함 확인)
    2. 중복 이메일로 회원 가입 시 실패 (400)
    3. 로그인 성공 (200 + 토큰 포함 확인)
    4. 존재하지 않는 이메일로 로그인 시 실패 (400)
    5. 잘못된 비밀번호로 로그인 시 실패 (400)
  - `MemberStepDefinitions.java`: 회원가입 When(POST /api/members/register), 로그인 When(POST /api/members/login), 토큰 포함 확인 Then(응답 JSON의 token 필드 검증)
- **Outcome**: `./gradlew cucumberTest` BUILD SUCCESSFUL (25 시나리오: gift 6 + member 5 + option 5 + product 3 + wish 6 전체 통과).

---

## 13단계: Docker PostgreSQL 도입 및 cucumber 프로필 분리

- **Prompt**: PostgreSQL 도입을 진행하자. docker-compose.yml에 PostgreSQL 15 서비스 정의(Healthcheck 포함)하고, application-cucumber.properties를 만들어 Docker PostgreSQL을 바라보도록 설정 분리해줘.
- **Action**:
  - `docker-compose.yml`: PostgreSQL 15 서비스 정의 (DB/User/Password: gift, 외부포트 25432→내부 5432, pg_isready healthcheck)
  - `src/main/resources/application-cucumber.properties`: PostgreSQL JDBC 연결 (localhost:25432/gift), Flyway 비활성화, ddl-auto=create-drop
- **Outcome**: Flyway migration SQL 파일이 존재하지 않아 cucumber 프로필도 기존 테스트와 동일하게 ddl-auto=create-drop 방식 적용.

---

## 14단계: Docker PostgreSQL 자동 라이프사이클 구성

- **Prompt**: cucumberTest 실행 시 Docker가 자동으로 뜨고 지워지게 해줘. docker-compose up → Cucumber 테스트(profile=cucumber) → docker-compose down 순서로.
- **Action**:
  - `build.gradle.kts`:
    - `runtimeOnly("org.postgresql:postgresql")` 의존성 추가
    - `dockerUp` 태스크 등록 (`docker compose up -d --wait`)
    - `dockerDown` 태스크 등록 (`docker compose down`, `isIgnoreExitValue = true`)
    - `cucumberTest`에 `dependsOn(dockerUp)`, `finalizedBy(dockerDown)`, `systemProperty("spring.profiles.active", "cucumber")` 추가
  - `CommonStepDefinitions.java`: H2 전용 `SET REFERENTIAL_INTEGRITY FALSE/TRUE` 제거 → PostgreSQL 호환 `TRUNCATE TABLE ... CASCADE`로 변경
- **Outcome**:
  - 첫 실행: `BadSqlGrammarException` — H2 전용 SQL이 PostgreSQL에서 실패. TRUNCATE CASCADE로 수정 후 해결.
  - 최종: `./gradlew cucumberTest` BUILD SUCCESSFUL (dockerUp→Healthy→25 시나리오 통과→dockerDown 정상 정리).

---

## 15단계: 애플리케이션 컨테이너화 (Dockerfile + docker-compose app 서비스)

- **Prompt**: Spring Boot 앱을 실행할 Dockerfile을 Multi-stage build로 작성하고, docker-compose.yml에 app 서비스를 추가해서 DB와 함께 실행되도록 해줘.
- **Action**:
  - `Dockerfile`: Multi-stage build (gradle:8.14-jdk21 빌드 → eclipse-temurin:21-jre 실행)
  - `docker-compose.yml`: app 서비스 추가 (외부포트 28080→내부 8080, 환경변수로 PostgreSQL 연결 설정, `depends_on: postgres: condition: service_healthy`)
- **Outcome**: Dockerfile 및 app 서비스 정의 완료. 포트 컨벤션 통일 (외부:내부 — DB 25432:5432, App 28080:8080).

---

## 16단계: Cucumber 테스트 전략 변경 (내부 서버 → Docker 앱)

- **Prompt**: Cucumber가 Docker에 떠 있는 외부 앱(localhost:28080)으로 요청을 보내도록 변경해줘. @SpringBootTest를 webEnvironment=NONE으로 변경.
- **Action**:
  - `CucumberSpringConfiguration.java`: `RANDOM_PORT` → `NONE` (테스트가 자체 웹서버를 띄우지 않음)
  - `CommonStepDefinitions.java`: `@LocalServerPort` 제거, `@Value`로 `cucumber.target.base-uri`/`cucumber.target.port` 주입
  - `application-cucumber.properties`: `ddl-auto=none` (스키마는 Docker 앱이 관리), `cucumber.target.*` 속성 추가, `jwt.secret` 통일
  - `docker-compose.yml`: app 서비스에 healthcheck 추가 (`/dev/tcp/localhost/8080`), `JWT_SECRET` 환경변수 추가
- **Outcome**:
  - 1차 실행: 25개 중 11개 실패 — gift/wish 시나리오(인증 필요) 전부 실패. 원인: 테스트의 jwt.secret(`...for-test`)과 Docker 앱의 기본값(`...long`)이 불일치하여 JWT 검증 실패.
  - 수정: `application-cucumber.properties`와 `docker-compose.yml`에 동일한 jwt.secret 설정.
  - 최종: `./gradlew cucumberTest` BUILD SUCCESSFUL (25 시나리오 전체 통과).
- **교훈**: 테스트가 외부 앱을 호출하는 구조에서는 JWT secret 같은 인증 설정이 양쪽에 동일해야 한다.

---

## 17단계: 스타일 정리 — Google Java Style 기반 코드 포맷 통일

- **Prompt**: Google Java Style 기반으로 코드 포맷 통일. 불필요한 공백/빈 줄/일관성 없는 들여쓰기 정리, import 순서 정리 및 미사용 import 제거.
- **Action**:
  - **Import 순서 정리 (12개 파일)**: Google Java Style(모든 non-static import를 하나의 블록으로, ASCII 알파벳 순)에 맞게 수정. 기존에는 IntelliJ 기본 설정으로 `java.*`/`javax.*`가 blank line으로 분리되어 있었음.
    - `OptionController.java`, `JwtProvider.java`, `CategoryController.java`, `ProductController.java`, `AdminProductController.java`, `WishController.java`, `OrderController.java`, `Product.java`, `Order.java`, `OptionRepository.java`, `WishRepository.java`, `MemberRepository.java`
  - **불필요한 빈 줄 제거 (1개 파일)**: `Member.java`에서 필드 선언 사이의 불필요한 빈 줄 제거 (다른 엔티티들과 일관성 맞춤)
  - **미사용 import**: 전수 검사 결과 없음 (`OrderController`의 `WishRepository` import은 필드에서 사용 중이므로 Dead Code 제거 단계에서 처리 예정)
- **Outcome**: `./gradlew clean build -x test` BUILD SUCCESSFUL, `./gradlew cucumberTest` BUILD SUCCESSFUL (25 시나리오 전체 통과). 작동 변경 없음 확인.

---

## 18단계: Dead Code 제거

- **Prompt**: IDE 정적 분석으로 미사용 필드/메서드/변수 식별. `OrderController`의 미사용 `WishRepository` 제거. 삭제 전 git blame으로 의도 확인.
- **Action**:
  - **git blame 확인**: `OrderController.java` 전체가 `wotjd243` (2026-02-18) 초기 커밋. 주석 `// 6. cleanup wish`로 계획만 존재, 구현 없음. `WishRepository`는 미구현 기능의 흔적.
  - **정적 분석 결과**:
    1. `OrderController.java`: 미사용 `WishRepository` (import, 필드, 생성자 파라미터) + 미구현 기능 주석 → **제거**
    2. `JwtProvider.java`, `AuthenticationResolver.java`, `MemberController.java`, `AdminMemberController.java`: 단일 생성자에 불필요한 `@Autowired` → **제거**
  - **수정 파일**: `OrderController.java`, `JwtProvider.java`, `AuthenticationResolver.java`, `MemberController.java`, `AdminMemberController.java`
- **Outcome**: `./gradlew clean build -x test` BUILD SUCCESSFUL, `./gradlew cucumberTest` BUILD SUCCESSFUL (25 시나리오 전체 통과). 작동 변경 없음 확인. README.md 체크박스 업데이트 완료.

---

## 19단계: OrderService 추출

- **Prompt**: `OrderService` 추출. 7단계 주문 흐름(재고 차감 + 포인트 차감 + 저장 + 카카오 알림)을 `@Transactional` 서비스로 이동.
- **Action**:
  - `OrderService.java` 신규 생성 (`@Service`):
    - `getOrders(Long memberId, Pageable)`: 주문 목록 조회 (repository 위임)
    - `createOrder(Member, Long optionId, int quantity, String message)`: `@Transactional` — 옵션 조회 → 재고 차감 → 포인트 차감 → 주문 저장 → 카카오 알림
    - `sendKakaoMessageIfPossible()`: private 헬퍼 (best-effort)
  - `OrderController.java` 수정:
    - 5개 의존성(OrderRepository, OptionRepository, MemberRepository, KakaoMessageClient) → `OrderService` 1개 + `AuthenticationResolver` 1개로 축소
    - 인증 체크만 컨트롤러에 유지, 비즈니스 로직은 서비스에 위임
    - 옵션 미존재 시 `NoSuchElementException` → 404 매핑
  - **설계 결정**:
    - 인증(auth check)은 HTTP 관심사이므로 컨트롤러에 유지
    - 서비스는 엔티티(`Order`)를 반환, 컨트롤러에서 DTO(`OrderResponse`)로 변환
    - `@Transactional`으로 포인트 차감 실패 시 재고 차감도 롤백 (기존 트랜잭션 부재 버그 수정 — chatlog 8단계에서 의도한 바)
- **Outcome**: `./gradlew clean build -x test` BUILD SUCCESSFUL, `./gradlew cucumberTest` BUILD SUCCESSFUL (25 시나리오 전체 통과).

---

## 20단계: MemberService 추출

- **Prompt**: `MemberService` 추출. `MemberController` + `AdminMemberController`의 중복 로직 통합.
- **Action**:
  - `MemberService.java` 신규 생성 (`@Service`):
    - `findAll()`, `findById(Long id)`: 조회
    - `register(String email, String password)`: 이메일 중복 체크 + 저장 (두 컨트롤러 공통 로직 통합)
    - `login(String email, String password)`: 이메일 조회 + 비밀번호 검증
    - `update(Long id, String email, String password)`: `@Transactional` — 조회 + 수정 + 저장
    - `chargePoint(Long id, int amount)`: `@Transactional` — 조회 + 충전 + 저장
    - `delete(Long id)`: 삭제
  - `MemberController.java` 수정: `MemberRepository` → `MemberService` 위임. register/login 로직 서비스로 이동.
  - `AdminMemberController.java` 수정: `MemberRepository` → `MemberService` 위임. create에서 `register()` 재사용 (중복 제거). findById 반복 코드 서비스의 `findById()` 하나로 통합.
  - **핵심 중복 제거**: 회원 생성 시 이메일 중복 체크 + 저장 로직이 `MemberService.register()`로 통합됨
- **Outcome**: `./gradlew clean build -x test` BUILD SUCCESSFUL, `./gradlew cucumberTest` BUILD SUCCESSFUL (25 시나리오 전체 통과).

---

## 21단계: ProductService 추출

- **Prompt**: `ProductService` 추출. `ProductController` + `AdminProductController`의 중복 로직 통합.
- **Action**:
  - `ProductService.java` 신규 생성 (`@Service`):
    - `findAll()`, `findAll(Pageable)`, `findById(Long)`: 조회 (NoSuchElementException)
    - `create(String name, int price, String imageUrl, Long categoryId)`: 카테고리 조회 + 저장
    - `update(Long id, ...)`: `@Transactional` — 상품·카테고리 조회 + 수정 + 저장
    - `delete(Long id)`: 삭제
  - `ProductController.java` 수정: `ProductRepository` + `CategoryRepository` → `ProductService` 1개로 축소. 이름 검증(`allowKakao=false`)은 컨트롤러에 유지.
  - `AdminProductController.java` 수정: `ProductRepository` → `ProductService` 위임. 이름 검증(`allowKakao=true`)과 폼 렌더링용 `CategoryRepository`는 컨트롤러에 유지.
  - **설계 결정**: 이름 검증 정책이 API/Admin에서 다르므로(`allowKakao` 차이) 검증은 컨트롤러에 유지, 핵심 CRUD만 서비스로 추출.
- **Outcome**: `./gradlew clean build -x test` BUILD SUCCESSFUL, `./gradlew cucumberTest` BUILD SUCCESSFUL (25 시나리오 전체 통과).

---

## 22단계: OptionService 추출

- **Prompt**: `OptionService` 추출. 이름 검증, 중복 체크, 최소 1개 규칙을 서비스로 이동.
- **Action**:
  - `OptionService.java` 신규 생성 (`@Service`):
    - `getOptions(Long productId)`: 상품 존재 확인 + 옵션 목록 조회
    - `create(Long productId, String name, int quantity)`: 이름 검증 + 상품 존재 확인 + 중복명 체크 + 저장
    - `delete(Long productId, Long optionId)`: 상품 존재 확인 + 최소 1개 규칙 + 옵션 존재/소속 확인 + 삭제
    - `validateName()`: private 헬퍼
  - `OptionController.java` 수정: `OptionRepository` + `ProductRepository` → `OptionService` 1개로 축소. 컨트롤러는 HTTP 매핑(NoSuchElementException→404, IllegalArgumentException→400)만 담당.
- **Outcome**: `./gradlew clean build -x test` BUILD SUCCESSFUL, `./gradlew cucumberTest` BUILD SUCCESSFUL (25 시나리오 전체 통과).

---

## 23단계: WishService 추출

- **Prompt**: `WishService` 추출. 중복 추가 방지, 소유권 검증을 서비스로 이동.
- **Action**:
  - `WishService.java` 신규 생성 (`@Service`):
    - `getWishes(Long memberId, Pageable)`: 위시 목록 조회 (페이징)
    - `findByMemberAndProduct(Long memberId, Long productId)`: 중복 확인용 Optional 반환
    - `addWish(Long memberId, Long productId)`: 상품 존재 확인 + 저장 (NoSuchElementException)
    - `removeWish(Long memberId, Long wishId)`: 위시 존재 확인 + 소유권 검증 + 삭제 (IllegalStateException→403)
  - `WishController.java` 수정: `WishRepository` + `ProductRepository` → `WishService` 1개로 축소. 컨트롤러는 인증 체크 + HTTP 매핑(NoSuchElementException→404, IllegalStateException→403)만 담당.
  - **설계 결정**: `findByMemberAndProduct`와 `addWish`를 분리하여 컨트롤러에서 200(기존 반환) vs 201(신규 생성) HTTP 상태코드를 구분할 수 있도록 함. 소유권 위반은 `IllegalStateException`으로 표현하여 `NoSuchElementException`(404)과 구분.
- **Outcome**: `./gradlew clean build -x test` BUILD SUCCESSFUL, `./gradlew cucumberTest` BUILD SUCCESSFUL (25 시나리오 전체 통과).

---

## 24단계: CategoryService 추출

- **Prompt**: `CategoryService` 추출. 단순 CRUD이지만 계층 일관성을 위해 추출.
- **Action**:
  - `CategoryService.java` 신규 생성 (`@Service`):
    - `findAll()`: 카테고리 목록 조회
    - `create(String name, String color, String imageUrl, String description)`: 저장
    - `update(Long id, ...)`: `@Transactional` — 조회 + 수정 + 저장 (NoSuchElementException)
    - `delete(Long id)`: 삭제
  - `CategoryController.java` 수정: `CategoryRepository` → `CategoryService` 1개로 축소. 컨트롤러는 HTTP 매핑(NoSuchElementException→404)만 담당.
  - **범위 판단**: `ProductService`와 `AdminProductController`가 직접 사용하는 `CategoryRepository`는 카테고리 ID로 조회하는 용도이므로 그대로 유지 (불필요한 의존성 우회 방지).
- **Outcome**: `./gradlew clean build -x test` BUILD SUCCESSFUL, `./gradlew cucumberTest` BUILD SUCCESSFUL (25 시나리오 전체 통과).

---

## 25단계: KakaoAuthService 추출

- **Prompt**: `KakaoAuthService` 추출. OAuth 콜백 흐름(토큰 교환 → 회원 조회/생성 → JWT 발급)을 서비스로 이동.
- **Action**:
  - `KakaoAuthService.java` 신규 생성 (`@Service`):
    - `buildAuthorizationUrl()`: 카카오 인가 URL 구성 (client_id, redirect_uri, scope 등)
    - `processCallback(String code)`: `@Transactional` — 인가 코드로 액세스 토큰 교환 → 사용자 정보 조회 → 회원 조회/생성 → 카카오 액세스 토큰 저장 → JWT 발급
  - `KakaoAuthController.java` 수정: `KakaoLoginProperties` + `KakaoLoginClient` + `MemberRepository` + `JwtProvider` 4개 의존성 → `KakaoAuthService` 1개로 축소. 컨트롤러는 HTTP 리다이렉트(302)와 응답(200 + TokenResponse)만 담당.
  - **설계 결정**: `login()`의 URL 구성도 OAuth 설정 로직이므로 서비스로 이동. `processCallback()`에 `@Transactional` 적용하여 회원 조회/생성 + 액세스 토큰 저장이 원자적으로 처리되도록 함.
- **Outcome**: `./gradlew clean build -x test` BUILD SUCCESSFUL, `./gradlew cucumberTest` BUILD SUCCESSFUL (25 시나리오 전체 통과).

---

## 26단계: AdminProductController의 CategoryRepository → CategoryService 교체

- **Prompt**: `AdminProductController`가 `CategoryRepository`를 직접 사용하는 마지막 잔존 케이스 제거. `CategoryService.findAll()`로 교체.
- **Action**:
  - `AdminProductController.java` 수정: `CategoryRepository` → `CategoryService`로 교체 (import, 필드, 생성자 파라미터, `findAll()` 호출 4곳)
  - 이로써 모든 컨트롤러가 Repository를 직접 참조하지 않고 Service만 의존하게 됨
- **Outcome**: `./gradlew clean build -x test` BUILD SUCCESSFUL, `./gradlew cucumberTest` BUILD SUCCESSFUL (25 시나리오 전체 통과).

---

## AI 활용 패턴 요약

### 전체 코드베이스 병렬 분석

3개의 탐색 에이전트를 병렬로 실행하여 두 프로젝트의 전체 코드베이스를 동시에 분석했다. 단일 순차 탐색 대비 분석 시간을 단축하고, 두 프로젝트 간의 구조적 차이를 빠르게 비교할 수 있었다.

```
에이전트 1: spring-gift-test-kakao 전체 분석
에이전트 2: spring-gift-refactoring-kakao 전체 분석    ← 병렬 실행
에이전트 3: 문서 체계 (CLAUDE.md, chatlog) 분석
```

### 이전 미션의 컨벤션 계승

이전 미션에서 확립한 문서 구조(`CLAUDE.md` + `docs/` + `chatlog/`)를 그대로 가져와 새 프로젝트의 도메인에 맞게 내용만 교체했다. 문서 구조 자체를 새로 설계하는 시간을 절약하고 일관성을 유지했다.

### 문제 발견 → 문서에 기록

코드 분석 과정에서 발견한 이슈(트랜잭션 부재, 위시 클린업 미구현, 인증 패턴 반복)를 TECH_SPEC.md와 FEATURES.md에 명시적으로 기록하여, 이후 리팩터링 단계에서 참조할 수 있도록 했다.
