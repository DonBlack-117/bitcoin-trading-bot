import { Panel, PanelHead, EmptyState } from './ui/Panel.jsx'
import Icon from './ui/Icon.jsx'
import { fmtMxn, fmtDateTime, toneOfSignal } from '../utils/format.js'
import './SignalHistory.css'

const LABELS = { COMPRAR: 'Comprar', VENDER: 'Vender', MANTENER: 'Mantener' }

function SignalHistory({ history }) {
  const items = history || []

  return (
    <Panel className="history-panel" coreClassName="history-core" aria-labelledby="history-title">
      <PanelHead eyebrow={`${items.length} registros`} title="Historial de señales" id="history-title" />

      {items.length === 0 ? (
        <EmptyState icon={<Icon name="inbox" size={22} />} title="Todavía no hay señales">
          El bot analiza el mercado cada 30 segundos y guarda la señal solo cuando cambia.
        </EmptyState>
      ) : (
        <ol className="history-list" data-testid="signal-history">
          {items.map((item, index) => {
            const tone = toneOfSignal(item.signal)
            return (
              <li key={item.id || index} className={`history-item tone-${tone}`}>
                <span className="history-dot" aria-hidden="true" />
                <div className="history-main">
                  <span className="history-signal">{LABELS[item.signal] || item.signal}</span>
                  <time className="history-date">{fmtDateTime(item.timestamp)}</time>
                </div>
                <div className="history-side">
                  <span className="history-price">{fmtMxn(item.price, 0)}</span>
                  <span className="history-conf">{item.confidence}%</span>
                </div>
              </li>
            )
          })}
        </ol>
      )}
    </Panel>
  )
}

export default SignalHistory
