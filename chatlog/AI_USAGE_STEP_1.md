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
