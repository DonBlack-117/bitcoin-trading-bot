import './IndicatorVotes.css'

const INDICATORS = [
  { key: 'emaCruce',           label: 'EMA Cruce',          subtitle: 'Tendencia SMA 50/200' },
  { key: 'rsiBollinger',       label: 'RSI + Bollinger',    subtitle: 'Sobrecomprado/Sobrevendido' },
  { key: 'macd',               label: 'MACD',               subtitle: 'Momentum' },
  { key: 'volumenVwap',        label: 'Volumen + VWAP',     subtitle: 'Confirmación de volumen' },
  { key: 'soporteResistencia', label: 'Soporte/Resistencia', subtitle: 'Niveles clave' },
]

function VoteDisplay({ value }) {
  const v = value || 0
  const abs = Math.abs(v)
  if (v > 0) return <span className="vote-badge vote-up">▲ SUBE {abs > 1 ? `(+${abs})` : ''}</span>
  if (v < 0) return <span className="vote-badge vote-down">▼ BAJA {abs > 1 ? `(${v})` : ''}</span>
  return <span className="vote-badge vote-neutral">— IGUAL</span>
}

function IndicatorVotes({ votes, scoreBuy, scoreSell }) {
  if (!votes) return null

  return (
    <div className="indicator-votes card">
      <div className="votes-header">
        <h2 className="votes-title">Votos de Estrategias</h2>
        <div className="votes-scores">
          <span className="score-pill score-buy">Alcista: {scoreBuy || 0}</span>
          <span className="score-pill score-sell">Bajista: {scoreSell || 0}</span>
        </div>
      </div>
      <div className="votes-grid">
        {INDICATORS.map(({ key, label, subtitle }) => (
          <div className="vote-item" key={key}>
            <div className="vote-info">
              <span className="vote-label">{label}</span>
              <span className="vote-subtitle">{subtitle}</span>
            </div>
            <VoteDisplay value={votes[key]} />
          </div>
        ))}
      </div>
    </div>
  )
}

export default IndicatorVotes
