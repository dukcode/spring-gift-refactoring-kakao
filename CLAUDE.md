# Claude Code Project Guidelines

## 1. Project Context
- **Name**: Gift Shop Refactoring (1단계: 리팩터링 준비하기)
- **Tech Stack**: Java 21, Spring Boot 3.5.9, JPA, Flyway, JWT, Kakao OAuth
- **Knowledge Base (Read when needed)**:
    - Architecture & APIs: `docs/TECH_SPEC.md` (프로젝트 구조, 현재 문제점)
    - Business Logic: `docs/FEATURES.md` (기능 명세서)
    - Testing Plan: `docs/TEST_STRATEGY.md` (테스트 전략)
    - Task & Strategy: `README.md` (구현 기능 목록, 리팩터링 계획, 구현 전략)

## 2. Common Commands
- Build: `./gradlew clean build -x test`
- Test: `./gradlew test`
- Run: `./gradlew bootRun`

## 3. Workflow Rules (CRITICAL)
1. **Metadata Router**: 코드를 외우지 말고, 로직이 궁금하면 `docs/` 폴더의 문서를 참조해.
2. **Auto-Logging**: 의미 있는 작업(설정 변경, 코드 구현, 버그 수정) 후에는 반드시 `chatlog/AI_USAGE_STEP_1.md`에 로그를 남겨.
    - **Format**:
      ```markdown
      ## [작업 단계] (예: 서비스 추출)
      - **Prompt**: (내가 요청한 내용 요약)
      - **Action**: (수정한 파일 및 내용)
      - **Outcome**: (결과 및 특이사항)
      ```
3. **Refactoring Rules**:
    - 구조 변경과 작동 변경을 섞지 않는다.
    - 한 번에 한 조각만 바꾼다.
    - 변경 후 반드시 테스트를 실행하여 작동이 유지됨을 확인한다.
    - AI가 요청하지 않은 작동을 추가하려 하면 즉시 멈춘다.
