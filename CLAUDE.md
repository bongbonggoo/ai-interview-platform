# AI 면접 연습 사이트 — 프로젝트 컨텍스트

Claude Code로 이 저장소를 열면 항상 먼저 읽어야 할 핵심 결정사항입니다.
전체 설계 논의(역량 라이브러리 근거, v1~v3 교정 과정)는 `erd.md`와
`korail_samuyoungeop_phase0_v3.xlsx`(별도 보관)를 참고하세요. 여기는 요약입니다.

## 프로젝트 개요

공기업 면접 연습 사이트. 핵심 차별점: **AI가 마음대로 채점하지 않고, 회사별로 확정된
루브릭(가중치+1/3/5점 행동기준)에 따라서만 채점**한다.

## 절대 지켜야 할 원칙 (코드 리뷰 시 이 기준으로 판단할 것)

1. **AI는 항목별 1~5점 + 근거만 낸다. 총점은 절대 AI가 계산하지 않는다.**
   총점 = 서비스 계층에서 `Σ(ai_score/5 × weight_pct)`로 계산. `EvaluationResult`에
   총점 컬럼을 추가하려는 시도가 나오면 이 원칙 위반인지 먼저 확인.
2. **역량(`CompetencyLibrary`)은 회사 무관 재사용 자산이다.** 회사별로 새 역량 이름을
   만들지 말 것 — 반드시 기존 canonical_id에 매칭하거나, 매칭 안 되면 사람 검토 큐
   (`CompetencyMatchCandidate`, 2차 작업)에 쌓는다.
3. **루브릭은 버전이 오른다(`RubricVersion`).** 앵커 문구를 고칠 때 기존 버전을 덮어쓰지
   말고 새 버전을 만든다. `InterviewSession`은 시작 시점의 `rubricVersion`을 고정
   참조하므로, 과거 세션의 채점 근거는 절대 안 바뀌어야 한다.
4. **AI 시뮬레이션 채점과 사람 채점을 같은 것으로 취급하지 않는다(2차 작업, `golden_score`
   테이블 만들 때 특히 주의).** `Rater.type = human`인 것만 `is_gold_confirmed = true`가
   될 수 있다.
5. **`Question`은 지금 Company에 종속(FK)돼 있다.** `QuestionTemplate` 분리는 의도적으로
   나중으로 미룸 — 지금 미리 일반화하지 말 것.

## 현재 상태 (Tier 1 완료)

- `Company`: Entity + Repository + Service + Controller 완성 (`POST/GET /companies` 동작 확인 대상)
- `JobPosition`, `CompetencyLibrary`, `RubricVersion`, `EvaluationCriterion`, `ScoreAnchor`,
  `Question`, `InterviewSession`, `InterviewTurn`, `InterviewAnswer`, `EvaluationResult`:
  Entity + Repository만 있음. Service/Controller 없음.
- User/인증 없음. `InterviewSession.userIdentifier`(문자열)로 임시 대체.
- 이 스캐폴딩은 Maven Central 접근이 막힌 샌드박스에서 작성되어 **컴파일 검증이 안 된
  상태**로 시작. 처음 빌드할 때 사소한 오류가 있으면 그때 고칠 것.

## 다음 작업 순서

1. `RubricVersion`/`EvaluationCriterion`/`ScoreAnchor`/`Question`용 Service+Controller 작성
2. `korail_samuyoungeop_phase0_v3.xlsx`의 04~07 시트 데이터를 seed로 삽입
   (Company → RubricVersion(코레일 v3, 5문항) → EvaluationCriterion → ScoreAnchor 순서로,
   FK 의존성 순서를 반드시 지킬 것)
3. `InterviewSession` 생성 → `InterviewTurn`/`InterviewAnswer` 저장 플로우
4. Claude API 연동 채점 서비스 (`EvaluationResult.aiScore`/`evidenceText` 채우기)
5. 총점 계산 + 결과 조회 API → 텍스트 면접 MVP 완성
6. 이후 2차 테이블(리서치 엔진: `research_snapshot`/`competency_evidence`/
   `competency_evidence_source`/`competency_alias`/`competency_match_candidate`,
   골든셋/하네스: `golden_answer`/`rater`/`golden_score`/`known_issue`) 순으로 확장
