# AI 면접 연습 사이트 — 백엔드

`erd.md` 기준 Tier 1(핵심 11개 테이블)의 JPA 엔티티 + Repository 위에,
**회사 → 질문 → 루브릭 버전 → 평가기준 → 행동기준(1/3/5)** 까지 API로 만들 수 있는 상태입니다.

핵심 차별점: AI가 마음대로 채점하지 않고, 회사별로 확정된 루브릭에 따라서만 채점한다.
그래서 "루브릭을 제대로 세울 수 있는가"가 먼저이고, 이 단계가 그 부분입니다.

## 구현 상태 (Tier 1)

| 테이블 | 상태 |
|---|---|
| company | Entity + Repository + Service + Controller |
| question | Entity + Repository + Service + Controller |
| competency_library | Entity + Repository + Service + Controller (evidence 체인은 2차) |
| rubric_version | Entity + Repository + Service + Controller (버전 자동 증가, verify) |
| evaluation_criterion | Entity + Repository + Service + Controller (가중치 검증) |
| score_anchor | Entity + Repository + Service + Controller (level별 upsert) |
| job_position | Entity + Repository + Service + Controller |
| interview_session | Entity + Repository + Service + Controller (루브릭 버전 고정) |
| interview_turn | Entity + Repository + Service + Controller (질문 문구 스냅샷) |
| interview_answer | Entity + Repository + Service + Controller (턴당 1회) |
| evaluation_result | Entity + Repository |

**아직 없는 것**: `research_snapshot`, `competency_evidence`, `competency_evidence_source`,
`competency_alias`, `competency_match_candidate`, `golden_answer`, `rater`, `golden_score`,
`known_issue` — 2차 작업으로 예정대로 미룸. 회원/인증(User)도 이번 범위에서 의도적으로 제외했고,
`InterviewSession.userIdentifier`(단순 문자열)로만 임시로 구분합니다.

## 실행 방법

```bash
./gradlew bootRun     # H2 인메모리 DB, 기본 프로필
./gradlew test        # 규칙 테스트 19개
```

띄운 뒤 **http://localhost:8080** 을 열면 개발용 콘솔이 나옵니다 — 샘플 데이터 생성부터
면접 시작 → 질문 → 답변 저장 → 결과 확인까지 버튼으로 따라가며 동작을 볼 수 있습니다
(실제 서비스 화면이 아니라 동작 확인용입니다).

Java 17 이상이면 됩니다(개발/검증은 21에서 했습니다). Gradle wrapper가 포함돼 있어 별도 설치는 필요 없습니다.

## API

### 회사 · 질문
```
POST   /companies                          {name, officialDomain}
GET    /companies
GET    /companies/{id}
POST   /companies/{companyId}/questions    {category, questionText, reusableTemplate}
GET    /companies/{companyId}/questions
GET    /questions/{id}
```

### 역량 라이브러리 (회사 무관 재사용 자산)
```
PUT    /competencies/{canonicalId}         {label, definition, tier}   # 멱등 upsert
GET    /competencies
GET    /competencies/{canonicalId}
```

### 직무
```
POST   /companies/{companyId}/job-positions     {name}
GET    /companies/{companyId}/job-positions
```

### 루브릭
```
POST   /companies/{companyId}/rubric-versions   {changelog}   # versionNumber는 서버가 max+1로 매김
GET    /companies/{companyId}/rubric-versions
GET    /rubric-versions/{id}
GET    /rubric-versions/{id}/validation         # 무엇이 verified를 막고 있는지 (상태 변경 없음)
POST   /rubric-versions/{id}/verify             # draft -> verified

POST   /rubric-versions/{id}/criteria      {questionId, canonicalId, weightPct, anchors:[{level, description}]}
GET    /rubric-versions/{id}/criteria
GET    /criteria/{id}

PUT    /criteria/{criterionId}/anchors     {level, description}   # level(1/3/5)별 멱등 upsert
GET    /criteria/{criterionId}/anchors
```

### 면접
```
POST   /interviews                         {userIdentifier, companyId, jobPositionId, rubricVersionId, interviewerStyle}
GET    /interviews/{sessionId}
GET    /interviews/{sessionId}/detail      # 세션 + 턴 + 답변 (채점기 입력)
GET    /interviews?userIdentifier=...
POST   /interviews/{sessionId}/end

POST   /interviews/{sessionId}/turns       {questionId}  또는  {questionText}  (꼬리질문)
GET    /interviews/{sessionId}/turns
POST   /interviews/{sessionId}/answers     {turnId, answerText, responseSeconds}
GET    /interviews/{sessionId}/turns/{turnId}/answer
```

`status`는 컬럼이 아니라 `endedAt`에서 파생됩니다 — `in_progress` / `completed`.
`questionId`를 주면 `questionTextSnapshot`은 서버가 `Question`에서 복사합니다(원본이 나중에
바뀌어도 그 세션에서 실제로 나간 문구는 그대로 남습니다). `orderIndex`는 세션별 자동 증가.

### 서비스 계층이 막아주는 것

| 규칙 | 응답 |
|---|---|
| 역량 라이브러리에 없는 `canonicalId` 참조 (회사별 새 역량 임의 생성 금지) | 404 |
| 한 질문의 `weight_pct` 합이 100 초과 | 422 |
| 가중치 합 ≠ 100 이거나 1/3/5 앵커 누락 상태로 verify | 422 |
| verified된 루브릭 버전의 criterion/anchor 수정 시도 | 409 (새 버전을 만들어야 함) |
| 같은 질문에 같은 역량 중복 등록 | 409 |
| 루브릭의 회사와 다른 회사의 질문 연결 | 400 |
| 앵커 level이 1/3/5가 아님 | 400 |
| verified 아닌 루브릭으로 면접 시작 | 422 |
| 세션의 회사와 다른 회사의 직무·루브릭·질문 연결 | 400 |
| 한 턴에 답변 두 번 제출 | 409 |
| 다른 세션의 턴에 답변 | 400 |
| 종료된 세션에 턴·답변 추가 | 409 |

## 동작 확인 (end-to-end)

```bash
# 1) 회사
COMPANY=$(curl -s -X POST localhost:8080/companies -H 'Content-Type: application/json' \
  -d '{"name":"한국철도공사","officialDomain":"info.korail.com"}' | jq -r .id)

# 2) 역량 (회사 무관 canonical 자산)
curl -s -X PUT localhost:8080/competencies/SAFETY_MINDSET -H 'Content-Type: application/json' \
  -d '{"label":"안전의식","definition":"규정 준수와 위험 인지","tier":"A"}'

# 3) 질문
QUESTION=$(curl -s -X POST localhost:8080/companies/$COMPANY/questions -H 'Content-Type: application/json' \
  -d '{"category":"직무","questionText":"원칙과 효율이 충돌한 경험을 말해주세요"}' | jq -r .id)

# 4) 루브릭 v1
RUBRIC=$(curl -s -X POST localhost:8080/companies/$COMPANY/rubric-versions -H 'Content-Type: application/json' \
  -d '{"changelog":"초안"}' | jq -r .id)

# 5) 평가기준 + 1/3/5 행동기준 (한 번에)
curl -s -X POST localhost:8080/rubric-versions/$RUBRIC/criteria -H 'Content-Type: application/json' \
  -d "{\"questionId\":\"$QUESTION\",\"canonicalId\":\"SAFETY_MINDSET\",\"weightPct\":100,\"anchors\":[
       {\"level\":1,\"description\":\"규정을 인지하지 못함\"},
       {\"level\":3,\"description\":\"규정을 지키는 수준\"},
       {\"level\":5,\"description\":\"선제적으로 위험을 차단함\"}]}"

# 6) 확정
curl -s localhost:8080/rubric-versions/$RUBRIC/validation   # {"valid":true,"problems":[]}
curl -s -X POST localhost:8080/rubric-versions/$RUBRIC/verify
```

## 다음 걸음

1. ~~`RubricVersion`/`EvaluationCriterion`/`ScoreAnchor`/`Question`용 서비스·컨트롤러~~ **완료**
2. `korail_samuyoungeop_phase0_v3.xlsx`의 04~07 시트 데이터를 seed로 삽입
   (`Company` → `CompetencyLibrary` → `Question`(Q1~Q5) → `RubricVersion`(코레일 v3) →
   `EvaluationCriterion` → `ScoreAnchor` 순)
3. ~~`InterviewSession` 생성 → `InterviewTurn`/`InterviewAnswer` 저장 흐름~~ **완료**
4. Claude API 연동 — `EvaluationResult.aiScore`/`evidenceText`를 채우는 채점 서비스
5. 총점 계산(서비스 계층, `Σ(ai_score/5 × weight_pct)`) 및 결과 조회 API
6. 여기까지 되면 텍스트 면접 MVP 완성 → 이후 2차 테이블(리서치 엔진, 골든셋, 하네스) 순으로 확장
