import { useEffect, useState } from 'react'

const MIN_LENGTH = 50

export default function App() {
  const [companies, setCompanies] = useState([])
  const [selected, setSelected] = useState(null)
  const [docType, setDocType] = useState('COVER_LETTER')
  const [question, setQuestion] = useState('')
  const [content, setContent] = useState('')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    fetch('/api/feedback/companies')
      .then((r) => r.json())
      .then(setCompanies)
      .catch(() => setError('백엔드에 연결하지 못했습니다. ./gradlew bootRun 이 켜져 있는지 확인하세요.'))
  }, [])

  const tooShort = content.trim().length < MIN_LENGTH
  const canSubmit = selected && !tooShort && !loading

  async function submit() {
    setLoading(true)
    setError(null)
    setResult(null)
    try {
      const res = await fetch('/api/feedback', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          companyName: selected.name,
          documentType: docType,
          question: question.trim() || null,
          content,
        }),
      })
      const data = await res.json()
      if (!res.ok) throw new Error(data.message || `요청이 실패했습니다 (${res.status})`)
      setResult(data)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="wrap">
      <h1>공기업 자소서·면접 피드백</h1>
      <p className="sub">
        기관을 고르고 글을 붙여넣으면, 그 기관의 인재상을 기준으로 어느 역량이 어디서 드러났고
        무엇이 약한지 짚어드립니다. 모범답안은 드리지 않습니다.
      </p>

      <div className="card">
        <div className="label">1. 지원 기관</div>
        <div className="companies">
          {companies.map((c) => (
            <button
              key={c.id}
              className="company"
              aria-pressed={selected?.id === c.id}
              onClick={() => setSelected(c)}
              title={c.competencyConfirmed ? c.aliases.join(', ') : '인재상 원문 미확인 — 공통 기준으로 평가합니다'}
            >
              {c.name}
              {!c.competencyConfirmed && <span className="mark">△</span>}
            </button>
          ))}
          {companies.length === 0 && <span className="sub">기관 목록을 불러오는 중…</span>}
        </div>
        {selected && !selected.competencyConfirmed && (
          <p className="criterion" style={{ marginBottom: 0 }}>
            △ 이 기관은 인재상 원문을 확보하지 못해 공기업 공통 역량 기준으로만 평가합니다.
          </p>
        )}
      </div>

      <div className="card">
        <div className="label">2. 글의 종류</div>
        <div className="toggle">
          <button aria-pressed={docType === 'COVER_LETTER'} onClick={() => setDocType('COVER_LETTER')}>
            자기소개서
          </button>
          <button aria-pressed={docType === 'INTERVIEW_ANSWER'} onClick={() => setDocType('INTERVIEW_ANSWER')}>
            면접 답변
          </button>
        </div>
      </div>

      <div className="card">
        <div className="label">3. {docType === 'COVER_LETTER' ? '문항' : '면접 질문'} (선택)</div>
        <input
          type="text"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder={
            docType === 'COVER_LETTER'
              ? '예) 지원동기와 입사 후 포부를 기술하시오'
              : '예) 원칙과 효율이 충돌한 경험을 말해주세요'
          }
        />

        <div className="label" style={{ marginTop: 18 }}>4. 내용</div>
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="작성한 글을 붙여넣으세요"
        />
        <div className="count">
          {content.trim().length}자 {tooShort && `· 최소 ${MIN_LENGTH}자`}
        </div>
      </div>

      <button className="submit" disabled={!canSubmit} onClick={submit}>
        {loading ? '읽는 중…' : selected ? `${selected.name} 기준으로 피드백 받기` : '기관을 먼저 선택하세요'}
      </button>

      {error && <p className="err">{error}</p>}

      {result && <Result data={result} />}
    </div>
  )
}

function Result({ data }) {
  return (
    <div style={{ marginTop: 26 }}>
      {data.scoredBy === 'stub' && (
        <div className="notice">
          스텁 생성기로 돌아가고 있습니다 — 글의 내용을 읽지 않은 결과입니다. 실제 피드백을 받으려면
          백엔드를 <code>ANTHROPIC_API_KEY</code> 와 함께 실행하세요.
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

            {c.strengths && (
              <div className="block">
                <div className="t">드러난 점</div>
                <p>{c.strengths}</p>
              </div>
            )}
            {c.gaps && (
              <div className="block">
                <div className="t">약한 점</div>
                <p>{c.gaps}</p>
              </div>
            )}
            {c.quotedEvidence && <blockquote>{c.quotedEvidence}</blockquote>}
          </div>
        ))}

        {data.sourceUrl && (
          <p className="source">
            인재상 출처:{' '}
            <a href={data.sourceUrl} target="_blank" rel="noreferrer">
              {data.sourceTitle || data.sourceUrl}
            </a>
          </p>
        )}
      </div>
    </div>
  )
}
