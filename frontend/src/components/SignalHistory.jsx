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
          El bot guarda una señal cada vez que analiza el mercado.
        </EmptyState>
      ) : (
        <ol className="history-list">
          {items.map((item, index) => {
            const tone = toneOfSignal(item.senal)
            return (
              <li key={item.id || index} className={`history-item tone-${tone}`}>
                <span className="history-dot" aria-hidden="true" />
                <div className="history-main">
                  <span className="history-signal">{LABELS[item.senal] || item.senal}</span>
                  <time className="history-date">{fmtDateTime(item.timestamp)}</time>
                </div>
                <div className="history-side">
                  <span className="history-price">{fmtMxn(item.precio, 0)}</span>
                  <span className="history-conf">{item.confianza}%</span>
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
