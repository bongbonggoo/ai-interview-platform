import { useEffect, useState } from 'react'

const MIN_LENGTH = 50

export default function App() {
  const [companies, setCompanies] = useState([])
  const [selected, setSelected] = useState(null)
  const [mode, setMode] = useState('QUESTIONS')
  const [error, setError] = useState(null)

  // 질문 생성
  const [jobTitle, setJobTitle] = useState('')
  const [count, setCount] = useState(5)
  const [questions, setQuestions] = useState(null)
  const [generating, setGenerating] = useState(false)

  // 답변 피드백
  const [docType, setDocType] = useState('INTERVIEW_ANSWER')
  const [question, setQuestion] = useState('')
  const [content, setContent] = useState('')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetch('/api/feedback/companies')
      .then((r) => r.json())
      .then(setCompanies)
      .catch(() => setError('백엔드에 연결하지 못했습니다. bootRun 이 켜져 있는지 확인하세요.'))
  }, [])

  async function post(path, body) {
    const res = await fetch(path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
    const data = await res.json()
    if (!res.ok) throw new Error(data.message || `요청이 실패했습니다 (${res.status})`)
    return data
  }

  async function generateQuestions() {
    setGenerating(true); setError(null); setQuestions(null)
    try {
      setQuestions(await post('/api/feedback/questions', {
        companyName: selected.name, jobTitle: jobTitle.trim() || null, count,
      }))
    } catch (e) { setError(e.message) } finally { setGenerating(false) }
  }

  function practice(q) {
    setQuestion(q.questionText)
    setDocType('INTERVIEW_ANSWER')
    setContent('')
    setResult(null)
    setMode('FEEDBACK')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  async function submitFeedback() {
    setLoading(true); setError(null); setResult(null)
    try {
      setResult(await post('/api/feedback', {
        companyName: selected.name, documentType: docType,
        question: question.trim() || null, content,
      }))
    } catch (e) { setError(e.message) } finally { setLoading(false) }
  }

  const tooShort = content.trim().length < MIN_LENGTH

  return (
    <div className="wrap">
      <h1>공기업 면접 연습</h1>
      <p className="sub">
        기관을 고르면 그 기관 인재상에 맞는 면접 질문을 만들어 드립니다.
        답변을 쓰면 같은 인재상 기준으로 어디가 드러났고 무엇이 약한지 짚어 드립니다.
        모범답안은 드리지 않습니다.
      </p>

      <div className="card">
        <div className="label">지원 기관</div>
        <div className="companies">
          {companies.map((c) => (
            <button key={c.id} className="company" aria-pressed={selected?.id === c.id}
              onClick={() => { setSelected(c); setQuestions(null); setResult(null) }}
              title={c.competencyConfirmed ? c.aliases.join(', ') : '인재상 원문 미확인 — 공통 기준으로 평가합니다'}>
              {c.name}{!c.competencyConfirmed && <span className="mark">△</span>}
            </button>
          ))}
          {companies.length === 0 && <span className="sub">기관 목록을 불러오는 중…</span>}
        </div>
      </div>

      {selected && (
        <>
          <div className="toggle" style={{ marginBottom: 14 }}>
            <button aria-pressed={mode === 'QUESTIONS'} onClick={() => setMode('QUESTIONS')}>
              면접 질문 만들기
            </button>
            <button aria-pressed={mode === 'FEEDBACK'} onClick={() => setMode('FEEDBACK')}>
              내 답변 피드백
            </button>
          </div>

          {mode === 'QUESTIONS'
            ? <QuestionMaker {...{ selected, jobTitle, setJobTitle, count, setCount,
                                   generating, generateQuestions, questions, practice }} />
            : <FeedbackForm {...{ selected, docType, setDocType, question, setQuestion,
                                  content, setContent, tooShort, loading, submitFeedback, result }} />}
        </>
      )}

      {error && <p className="err">{error}</p>}
    </div>
  )
}

function QuestionMaker({ selected, jobTitle, setJobTitle, count, setCount,
                         generating, generateQuestions, questions, practice }) {
  return (
    <>
      <div className="card">
        <div className="label">직무 (선택)</div>
        <input type="text" value={jobTitle} onChange={(e) => setJobTitle(e.target.value)}
          placeholder="예) 사무영업, 토목, 전기" />

        <div className="label" style={{ marginTop: 18 }}>질문 개수</div>
        <div className="toggle">
          {[3, 5, 8].map((n) => (
            <button key={n} aria-pressed={count === n} onClick={() => setCount(n)}>{n}개</button>
          ))}
        </div>
      </div>

      <button className="submit" disabled={generating} onClick={generateQuestions}>
        {generating ? '만드는 중…' : `${selected.name} 면접 질문 만들기`}
      </button>

      {questions && (
        <div style={{ marginTop: 26 }}>
          {questions.generatedBy === 'stub' && (
            <div className="notice">
              스텁으로 만든 틀 문장입니다 — 기관 맥락이 반영되지 않았습니다.
              실제 질문을 받으려면 백엔드를 API 키와 함께 실행하세요.
            </div>
          )}
          {!questions.competencyConfirmed && (
            <div className="notice info">
              {questions.companyName}의 인재상 원문을 확보하지 못해 공기업 공통 역량으로 만들었습니다.
            </div>
          )}

          <div className="card">
            {questions.questions.map((q, i) => (
              <div className="comp" key={q.questionId}>
                <div className="comp-head">
                  <span className="badge">Q{i + 1}</span>
                  <strong style={{ fontSize: 15, fontWeight: 500 }}>{q.questionText}</strong>
                </div>
                <p className="criterion">
                  {q.targets.map((t) => t.companyLabel).join(' · ')}
                  {q.intent && ` — ${q.intent}`}
                </p>
                <button className="company" onClick={() => practice(q)}>이 질문으로 답변 연습 →</button>
              </div>
            ))}
            {questions.sourceUrl && (
              <p className="source">
                인재상 출처: <a href={questions.sourceUrl} target="_blank" rel="noreferrer">
                  {questions.sourceTitle || questions.sourceUrl}</a>
              </p>
            )}
          </div>
        </div>
      )}
    </>
  )
}

function FeedbackForm({ selected, docType, setDocType, question, setQuestion,
                        content, setContent, tooShort, loading, submitFeedback, result }) {
  return (
    <>
      <div className="card">
        <div className="label">글의 종류</div>
        <div className="toggle">
          <button aria-pressed={docType === 'INTERVIEW_ANSWER'} onClick={() => setDocType('INTERVIEW_ANSWER')}>
            면접 답변
          </button>
          <button aria-pressed={docType === 'COVER_LETTER'} onClick={() => setDocType('COVER_LETTER')}>
            자기소개서
          </button>
        </div>

        <div className="label" style={{ marginTop: 18 }}>
          {docType === 'COVER_LETTER' ? '문항' : '면접 질문'} (선택)
        </div>
        <input type="text" value={question} onChange={(e) => setQuestion(e.target.value)}
          placeholder={docType === 'COVER_LETTER'
            ? '예) 지원동기와 입사 후 포부를 기술하시오'
            : '질문 만들기 탭에서 질문을 고르면 자동으로 채워집니다'} />

        <div className="label" style={{ marginTop: 18 }}>내용</div>
        <textarea value={content} onChange={(e) => setContent(e.target.value)}
          placeholder="답변을 작성하세요" />
        <div className="count">{content.trim().length}자 {tooShort && `· 최소 ${MIN_LENGTH}자`}</div>
      </div>

      <button className="submit" disabled={tooShort || loading} onClick={submitFeedback}>
        {loading ? '읽는 중…' : `${selected.name} 기준으로 피드백 받기`}
      </button>

      {result && <Result data={result} />}
    </>
  )
}

function Result({ data }) {
  return (
    <div style={{ marginTop: 26 }}>
      {data.scoredBy === 'stub' && (
        <div className="notice">
          스텁으로 돌아가고 있습니다 — 글의 내용을 읽지 않은 결과입니다.
          실제 피드백을 받으려면 백엔드를 API 키와 함께 실행하세요.
        </div>
      )}
      {!data.competencyConfirmed && (
        <div className="notice info">
          {data.companyName}의 인재상 원문을 확보하지 못해 공기업 공통 역량 기준으로 평가했습니다.
        </div>
      )}

      <div className="card">
        <div className="total">
          <b>{data.totalScore ?? '-'}</b>
          <span>/ 100 · 역량별 점수를 평균해 서버가 계산한 값입니다</span>
        </div>
        <p className="overall">{data.overall}</p>
      </div>

      <div className="card">
        {data.competencies.map((c) => (
          <div className="comp" key={c.canonicalId}>
            <div className="comp-head">
              <span className="badge">{c.score}점</span>
              <strong>{c.companyLabel}</strong>
              {c.companyLabel !== c.competencyLabel && (
                <span className="comp-sub">공통 역량: {c.competencyLabel}</span>
              )}
            </div>
            <p className="criterion">기준 · {c.scoreCriterion}</p>
            {c.strengths && <div className="block"><div className="t">드러난 점</div><p>{c.strengths}</p></div>}
            {c.gaps && <div className="block"><div className="t">약한 점</div><p>{c.gaps}</p></div>}
            {c.quotedEvidence && <blockquote>{c.quotedEvidence}</blockquote>}
          </div>
        ))}
        {data.sourceUrl && (
          <p className="source">
            인재상 출처: <a href={data.sourceUrl} target="_blank" rel="noreferrer">
              {data.sourceTitle || data.sourceUrl}</a>
          </p>
        )}
      </div>
    </div>
  )
}
