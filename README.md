# AI 면접 연습 사이트 — 백엔드 (1차 스캐폴딩)

`erd.md` 기준 Tier 1(핵심 11개 테이블)의 JPA 엔티티 + Repository, 그리고 첫 성공 기준인
`Company` CRUD(Controller/Service/DTO)까지 구현한 상태입니다.

## 포함된 것 (Tier 1)

| 테이블 | 상태 |
|---|---|
| company | Entity + Repository + Service + Controller (완성) |
| job_position | Entity + Repository |
| competency_library | Entity + Repository (evidence 체인은 2차) |
| rubric_version | Entity + Repository |
| evaluation_criterion | Entity + Repository |
| score_anchor | Entity + Repository |
| question | Entity + Repository |
| interview_session | Entity + Repository |
| interview_turn | Entity + Repository |
| interview_answer | Entity + Repository |
| evaluation_result | Entity + Repository |

**아직 없는 것**: `research_snapshot`, `competency_evidence`, `competency_evidence_source`,
`competency_alias`, `competency_match_candidate`, `golden_answer`, `rater`, `golden_score`,
`known_issue` — 2차 작업으로 예정대로 미룸. 회원/인증(User)도 이번 범위에서 의도적으로 제외했고,
`InterviewSession.userIdentifier`(단순 문자열)로만 임시로 구분합니다.

## 실행 방법

이 스캐폴딩은 샌드박스 환경(Maven Central 접근 불가)에서 작성되어 **여기서 컴파일 검증을 하지
못했습니다.** 로컬에서:

```bash
# 1) Gradle wrapper가 없으므로 최초 1회 생성 (로컬에 Gradle이 설치되어 있다면)
gradle wrapper

# 2) 실행 (H2 인메모리 DB, 기본 프로필)
./gradlew bootRun
```

또는 IntelliJ에서 폴더를 Gradle 프로젝트로 열면 wrapper 없이도 바로 인식·실행됩니다.

## 1차 성공 기준 확인

```bash
curl -X POST localhost:8080/companies \
  -H "Content-Type: application/json" \
  -d '{"name":"한국철도공사","officialDomain":"info.korail.com"}'

# 응답의 id를 가지고
curl localhost:8080/companies/{id}
```

## 다음 걸음

1. `RubricVersion` / `EvaluationCriterion` / `ScoreAnchor` / `Question`용 서비스·컨트롤러 작성
2. `korail_samuyoungeop_phase0_v3.xlsx`의 04~07 시트 데이터를 이 스키마에 맞춰 seed로 삽입
   (`Company` → `RubricVersion`(코레일 v3) → `Question`(Q1~Q5) → `EvaluationCriterion` → `ScoreAnchor` 순)
3. `InterviewSession` 생성 → `InterviewTurn`/`InterviewAnswer` 저장 흐름 구현
4. Claude API 연동 — `EvaluationResult.aiScore`/`evidenceText`를 채우는 채점 서비스
5. 총점 계산(서비스 계층, `Σ(ai_score/5 × weight_pct)`) 및 결과 조회 API
6. 여기까지 되면 텍스트 면접 MVP 완성 → 이후 2차 테이블(리서치 엔진, 골든셋, 하네스) 순으로 확장
