import './SignalCard.css'

function SignalCard({ signal }) {
  if (!signal) return null

  const { emoji, signal: signalText, description, cssClass, color, confidence, rsi } = signal

  return (
    <div className={`signal-card card ${cssClass}`}>
      <div className="signal-header">
        <span className="signal-emoji">{emoji}</span>
        <div>
          <div className="signal-label">Señal Actual</div>
          <div className="signal-text" style={{ color }}>
            {signalText}
          </div>
        </div>
      </div>

      <p className="signal-description">{description}</p>

      <div className="confidence-section">
        <div className="confidence-label">
          <span>Confianza</span>
          <span className="confidence-value" style={{ color }}>{confidence}%</span>
        </div>
        <div className="confidence-bar-bg">
          <div
            className="confidence-bar-fill"
            style={{ width: `${confidence}%`, backgroundColor: color }}
          ></div>
        </div>
      </div>

      <div className="rsi-context">
        <span className="rsi-label">RSI actual: </span>
        <span className="rsi-value" style={{ color: getRsiColor(rsi) }}>
          {rsi ? rsi.toFixed(1) : 'N/A'}
        </span>
        <span className="rsi-interpretation"> — {getRsiText(rsi)}</span>
      </div>
    </div>
  )
}

function getRsiColor(rsi) {
  if (!rsi) return '#6b7280'
  if (rsi < 35) return '#22c55e'
  if (rsi > 65) return '#ef4444'
  return '#f59e0b'
}

function getRsiText(rsi) {
  if (!rsi) return 'Sin datos'
  if (rsi < 30) return 'Muy sobrevendido'
  if (rsi < 40) return 'Sobrevendido'
  if (rsi > 70) return 'Muy sobrecomprado'
  if (rsi > 60) return 'Sobrecomprado'
  return 'Zona neutral'
}

export default SignalCard
