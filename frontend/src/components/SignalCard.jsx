import { Panel, PanelHead } from './ui/Panel.jsx'
import Icon from './ui/Icon.jsx'
import { toneOfSignal, fmtMxn, fmtNum } from '../utils/format.js'
import './SignalCard.css'

const TONE_ICON = { buy: 'arrowUp', sell: 'arrowDown', hold: 'minus' }

function rsiText(rsi) {
  if (!rsi) return 'Sin datos'
  if (rsi < 30) return 'Muy sobrevendido'
  if (rsi < 40) return 'Sobrevendido'
  if (rsi > 70) return 'Muy sobrecomprado'
  if (rsi > 60) return 'Sobrecomprado'
  return 'Zona neutral'
}

function SignalCard({ signal }) {
  if (!signal) return null

  const { signal: signalText, description, cssClass, confidence, rsi, indicators = {} } = signal
  const macdHist = indicators.macdHistogram
  const tone = toneOfSignal(signalText, cssClass)
  const rsiPos = Math.min(100, Math.max(0, rsi || 0))

  return (
    <Panel className={`signal-panel tone-${tone}`} aria-labelledby="signal-title">
      <PanelHead eyebrow="Señal del bot" title="Qué hacer ahora" id="signal-title" />

      <div className="signal-main">
        <span className="signal-icon" aria-hidden="true">
          <Icon name={TONE_ICON[tone]} size={26} />
        </span>
        <p className="signal-word">{signalText}</p>
        {tone === 'hold' && signalText !== 'MANTENER' && (
          <span className="signal-weak" title="Confianza menor a 60%: el bot no opera con esta señal">Débil</span>
        )}
      </div>

      <p className="signal-desc">{description}</p>

      <div className="meter">
        <div className="meter-head">
          <span className="stat-label">Confianza</span>
          <span className="meter-value">{confidence}%</span>
        </div>
        <div className="meter-track" role="meter" aria-valuemin={0} aria-valuemax={100} aria-valuenow={confidence} aria-label="Confianza de la señal">
          <span className="meter-fill" style={{ transform: `scaleX(${(confidence || 0) / 100})` }} />
        </div>
      </div>

      <div className="rsi">
        <div className="meter-head">
          <span className="stat-label">RSI (14)</span>
          <span className="meter-value">
            {rsi ? rsi.toFixed(1) : 'N/A'}
            <span className="rsi-text">{rsiText(rsi)}</span>
          </span>
        </div>
        <div className="rsi-track" role="img" aria-label={`RSI ${rsi ? rsi.toFixed(1) : 'sin datos'}: ${rsiText(rsi)}`}>
          <span className="rsi-zone rsi-zone-low" />
          <span className="rsi-zone rsi-zone-high" />
          {rsi ? <span className="rsi-marker" style={{ left: `${rsiPos}%` }} /> : null}
        </div>
        <div className="rsi-scale" aria-hidden="true">
          <span>0</span><span>30</span><span>70</span><span>100</span>
        </div>
      </div>

      <dl className="signal-indicators">
        <div>
          <dt>MACD (histograma)</dt>
          <dd className={macdHist > 0 ? 'pnl-up' : macdHist < 0 ? 'pnl-down' : ''}>
            {macdHist != null ? `${macdHist > 0 ? '+' : ''}${fmtNum(macdHist, 0)}` : '—'}
          </dd>
        </div>
        <div>
          <dt>ATR (volatilidad)</dt>
          <dd>{fmtMxn(indicators.atr, 0)}</dd>
        </div>
        <div>
          <dt>SMA 50</dt>
          <dd>{fmtMxn(indicators.sma50, 0)}</dd>
        </div>
      </dl>
    </Panel>
  )
}

export default SignalCard
