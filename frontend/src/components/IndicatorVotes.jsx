import { Panel, PanelHead } from './ui/Panel.jsx'
import './IndicatorVotes.css'

const INDICATORS = [
  { key: 'emaCruce',           label: 'Cruce de medias',     subtitle: 'Tendencia SMA 50/200' },
  { key: 'rsiBollinger',       label: 'RSI + Bollinger',     subtitle: 'Sobrecompra y sobreventa' },
  { key: 'macd',               label: 'MACD',                subtitle: 'Momentum' },
  { key: 'volumenVwap',        label: 'Volumen + VWAP',      subtitle: 'Confirmación de volumen' },
  { key: 'soporteResistencia', label: 'Soporte/Resistencia', subtitle: 'Niveles clave' },
]

const MAX_VOTE = 3

function voteWord(v) {
  if (v > 0) return 'Alcista'
  if (v < 0) return 'Bajista'
  return 'Neutral'
}

function IndicatorVotes({ votes, scoreBuy = 0, scoreSell = 0 }) {
  if (!votes) return null

  const net = scoreBuy - scoreSell

  return (
    <Panel className="votes-panel" aria-labelledby="votes-title">
      <PanelHead eyebrow="Cinco estrategias, de −3 a +3" title="Votos de estrategias" id="votes-title">
        <div className="votes-score">
          <span className="votes-score-item is-up">Alcista <b>{scoreBuy}</b></span>
          <span className="votes-score-item is-down">Bajista <b>{scoreSell}</b></span>
          <span className="votes-score-item">Neto <b>{net > 0 ? `+${net}` : net}</b></span>
        </div>
      </PanelHead>

      <ul className="votes-list">
        {INDICATORS.map(({ key, label, subtitle }, i) => {
          const v = votes[key] || 0
          const dir = v > 0 ? 'up' : v < 0 ? 'down' : 'flat'
          const scale = Math.min(Math.abs(v), MAX_VOTE) / MAX_VOTE
          return (
            <li className={`vote-row vote-${dir}`} key={key} style={{ '--i': i }}>
              <div className="vote-info">
                <span className="vote-label">{label}</span>
                <span className="vote-sub">{subtitle}</span>
              </div>
              <div className="vote-bar" role="img" aria-label={`${label}: ${voteWord(v)} ${v > 0 ? '+' : ''}${v}`}>
                <span className="vote-axis" />
                <span className="vote-fill" style={{ transform: `scaleX(${scale})` }} />
              </div>
              <div className="vote-result">
                <span className="vote-num">{v > 0 ? `+${v}` : v}</span>
                <span className="vote-word">{voteWord(v)}</span>
              </div>
            </li>
          )
        })}
      </ul>
      <div className="vote-scale" aria-hidden="true">
        <span>−3 bajista</span><span>0</span><span>+3 alcista</span>
      </div>
    </Panel>
  )
}

export default IndicatorVotes
