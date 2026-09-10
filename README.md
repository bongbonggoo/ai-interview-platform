# AI 면접 연습 사이트 — 백엔드

## 핵심 기능

**기관을 고르면 그 기관 인재상에 맞는 면접 질문을 만들어 줍니다.** 답변을 쓰면 같은 인재상
기준으로 어느 역량이 어디서 드러났고 무엇이 약한지 원문을 짚어가며 피드백합니다.

```
GET  /api/feedback/companies     # 선택 가능한 기관 목록

POST /api/feedback/questions     # 면접 질문 생성 (생성한 질문은 저장됨)
{ "companyName": "코레일", "jobTitle": "사무영업", "count": 5 }

POST /api/feedback               # 답변·자소서 피드백
{ "companyName": "코레일", "documentType": "INTERVIEW_ANSWER",
  "question": "...", "content": "..." }
```

질문에는 모범답안이나 답변 힌트가 따라오지 않습니다. 어떤 역량을 보는 질문인지(`targets`)와
무엇을 확인하려는지(`intent`)까지만 보여줍니다.

**모범답안은 제공하지 않습니다.** 채점 기준은 "이 역량이 어떻게 드러났는가"를 보는 행동기준
(1/3/5)이고, 부족한 부분은 짚어주되 대신 채워주지 않습니다.

### 미리 넣어둔 기관 (9곳)

| 기관 | 별칭 | 인재상 |
|---|---|---|
| 한국철도공사 | 코레일, KORAIL | 사람지향 소통인 · 고객지향 전문인 · 미래지향 혁신인 |
| 한국전력공사 | 한전, KEPCO | 통섭형 · 기업가형 · 가치창조형 · 도전형 |
| 한국수력원자력 | 한수원, KHNP | 안전 최우선 · 지속 성장 · 상호 존중 · 사회적 책임 |
| 건강보험심사평가원 | 심평원, HIRA | 국민 최우선 · 공정과 신뢰 · 소통과 협력 · 열린 전문성 |
| 국민건강보험공단 | 건보공단, NHIS | 국민지향 · 정직 · 혁신 · 전문성 |
| 부산항만공사 | BPA | 혁신 · 전문성 · 상생 · 소통 |
| 대한무역투자진흥공사 | KOTRA, 코트라 | 글로벌 · 혁신 · 공헌 · 공정·청렴 |
| 도로교통공단 | KoROAD | **원문 미확인** — 공통 기준으로만 평가 |
| 금융감독원 | 금감원, FSS | **원문 미확인** — 공통 기준으로만 평가 |

인재상은 출처를 확인한 것만 실었고, 응답에 `sourceUrl`로 함께 나갑니다.
확인하지 못한 두 기관은 추측으로 채우지 않고 `competencyConfirmed: false`로 두었습니다.
공통 기준은 NCS 직업기초능력 기반 17개 역량 라이브러리에서 가져옵니다.


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
| evaluation_result | Entity + Repository + Service + Controller (AI 채점, 총점 계산) |

**아직 없는 것**: `research_snapshot`, `competency_evidence`, `competency_evidence_source`,
`competency_alias`, `competency_match_candidate`, `golden_answer`, `rater`, `golden_score`,
`known_issue` — 2차 작업으로 예정대로 미룸. 회원/인증(User)도 이번 범위에서 의도적으로 제외했고,
`InterviewSession.userIdentifier`(단순 문자열)로만 임시로 구분합니다.

## 실행 방법

**명령 하나면 됩니다.** 프론트엔드까지 같이 빌드해서 8080 한 포트로 서빙합니다.

```bash
./gradlew bootRun            # macOS / Linux
.\gradlew.bat bootRun        # Windows
```

브라우저에서 **http://localhost:8080** 을 엽니다. Node가 없어도 Gradle이 알아서 받아오므로
npm을 따로 설치할 필요가 없습니다. Java 17 이상만 있으면 됩니다(Java 25까지 확인).

### 실제 피드백을 받으려면 API 키

키가 없으면 스텁으로 떠서 글을 읽지 않습니다(화면과 흐름 확인은 가능). 둘 중 하나를 넣으면 됩니다.

```bash
GEMINI_API_KEY=... ./gradlew bootRun        # 무료 티어 있음
ANTHROPIC_API_KEY=... ./gradlew bootRun     # 유료, 건당 5센트 안팎
```

Windows 명령 프롬프트에서는 `set GEMINI_API_KEY=...` 를 먼저 실행한 뒤 `.\gradlew.bat bootRun`.

| | 발급 | 비용 |
|---|---|---|
| **Gemini** (권장) | [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey) — 구글 계정만, 결제수단 불필요 | 무료 티어 (분당·일일 요청 수 제한) |
| **Claude** | [console.anthropic.com](https://console.anthropic.com) — 결제수단 등록 필요 | Opus 5 기준 자소서 1건당 약 5센트 |

둘 다 있으면 Claude를 씁니다. 어느 쪽으로 채점됐는지는 응답의 `scoredBy`에 드러납니다.

### 개발 중 프론트만 따로 (선택)

화면을 고치면서 즉시 반영되게 하려면 Vite 개발 서버를 따로 띄웁니다.

```bash
cd frontend && npm install && npm run dev   # http://localhost:5173
```

`/api` 요청은 Vite가 8080으로 넘겨줍니다.

```bash
./gradlew test        # 규칙 테스트 46개
```

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

### 채점
```
POST   /interviews/{sessionId}/turns/{turnId}/evaluation   # 턴 하나 채점
POST   /interviews/{sessionId}/evaluate                    # 미채점 답변 일괄 채점
GET    /interviews/{sessionId}/result                      # 역량별 점수 + 근거 + 총점
```

**AI는 항목별 1~5점과 근거만 냅니다.** 총점은 `ScoreCalculator`가
`Σ(ai_score / 5 × weight_pct)`로 계산하며 DB에 저장하지 않습니다 — 조회할 때마다 다시 계산합니다.
프롬프트에는 그 세션에 고정된 루브릭 버전의 criterion과 1/3/5 앵커만 들어갑니다.

채점기는 API 키 유무로 갈립니다:

스텁은 답변 길이로만 점수를 매기는 대역이라 실제 평가에 쓰면 안 됩니다.
모델은 `ai.claude.model`(기본 `claude-opus-5`) 또는 `ai.gemini.model`(기본 `gemini-3.6-flash`)로 바꿉니다.

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
| 이미 채점된 답변 재채점 | 409 |
| 답변 없는 턴 / 꼬리질문 채점 | 422 |
| 루브릭에 그 질문의 평가 기준이 없음 | 422 |
| AI가 1~5 밖의 점수를 내거나 기준을 빠뜨림 | 502 (저장 안 함) |

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
4. ~~Claude API 연동 — `EvaluationResult.aiScore`/`evidenceText`를 채우는 채점 서비스~~ **완료**
5. ~~총점 계산 및 결과 조회 API~~ **완료** → 텍스트 면접 MVP 동작
6. 남은 것: 실제 Claude 채점 품질 확인(키를 넣고 코레일 시드로 검증), 회원/인증,
   그리고 2차 테이블(리서치 엔진: `research_snapshot`/`competency_evidence`/
   `competency_evidence_source`/`competency_alias`/`competency_match_candidate`,
   골든셋/하네스: `golden_answer`/`rater`/`golden_score`/`known_issue`)
