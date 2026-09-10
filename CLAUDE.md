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

## 현재 상태

- **루브릭 세우기**(Company / JobPosition / Question / CompetencyLibrary / RubricVersion /
  EvaluationCriterion / ScoreAnchor)와 **면접 응시**(InterviewSession / InterviewTurn /
  InterviewAnswer): Entity + Repository + Service + Controller 완성.
- `EvaluationResult`: Entity + Repository만 있음. 채점 서비스가 다음 작업.
- `src/main/resources/static/index.html`: 개발용 콘솔(전체 흐름 버튼으로 확인). 서비스 화면 아님.
- User/인증 없음. `InterviewSession.userIdentifier`(문자열)로 임시 대체.
- 컴파일·기동·테스트 검증 완료(Java 21, `./gradlew test` 19 passed). Gradle wrapper 포함.

## 원칙이 코드로 집행되는 지점 (여기를 무력화하는 변경은 리뷰에서 막을 것)

| 원칙 | 집행 위치 |
|---|---|
| 2. 회사별 새 역량 금지 | `CompetencyLibraryService.getEntity` — 없는 canonical_id는 404, 자동 생성하지 않음 |
| 3. 루브릭은 덮어쓰지 않고 버전이 오른다 | `RubricVersionService.assertMutable` — verified 버전 하위 수정은 전부 409 |
| 3. 버전 번호는 사람이 정하지 않는다 | `RubricVersionService.create` — 회사별 max+1 자동 |
| 가중치 합 100 (워크북 SUMIF의 대체물) | `EvaluationCriterionService.create`(>100 즉시 거부) + `RubricVersionService.verify`(=100 요구) |
| 1/3/5 BARS 앵커 강제 | `ScoreAnchorService`(level 화이트리스트) + `verify`(세 level 다 있어야 통과) |
| 3. 세션은 시작 시점 루브릭을 고정 | `InterviewSessionService.create` — verified 루브릭만 허용(422), 이후 버전이 올라가도 세션은 안 바뀜 |
| 실제로 물어본 문구 보존 | `InterviewTurnService` — 스냅샷을 클라이언트가 아니라 Question에서 복사 |
| 답변 사후 변경 금지 | `InterviewAnswerService` — 턴당 1회, 재제출은 409 |

이 규칙들은 `src/test/java/com/aiinterview/rubric/RubricFlowTest.java`에 테스트로 고정돼 있다.
규칙을 바꿔야 한다면 테스트부터 고치고, 왜 바꾸는지 근거를 남길 것.

## 다음 작업 순서

1. ~~`RubricVersion`/`EvaluationCriterion`/`ScoreAnchor`/`Question`용 Service+Controller~~ **완료**
2. `korail_samuyoungeop_phase0_v3.xlsx`의 04~07 시트 데이터를 seed로 삽입
   (Company → CompetencyLibrary → Question(Q1~Q5) → RubricVersion(코레일 v3) →
   EvaluationCriterion → ScoreAnchor 순서로, FK 의존성 순서를 반드시 지킬 것)
3. ~~`InterviewSession` 생성 → `InterviewTurn`/`InterviewAnswer` 저장 플로우~~ **완료**
4. Claude API 연동 채점 서비스 (`EvaluationResult.aiScore`/`evidenceText` 채우기).
   프롬프트에는 해당 세션의 고정된 `rubricVersion`에 딸린 criterion + 1/3/5 앵커만 넣는다.
5. 총점 계산 + 결과 조회 API → 텍스트 면접 MVP 완성.
   총점은 서비스 계층 `Σ(ai_score/5 × weight_pct)` — AI에게 계산시키지 않는다(원칙 1).
6. 이후 2차 테이블(리서치 엔진: `research_snapshot`/`competency_evidence`/
   `competency_evidence_source`/`competency_alias`/`competency_match_candidate`,
   골든셋/하네스: `golden_answer`/`rater`/`golden_score`/`known_issue`) 순으로 확장
