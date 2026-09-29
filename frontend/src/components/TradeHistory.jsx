import { Panel, PanelHead, EmptyState } from './ui/Panel.jsx'
import Icon from './ui/Icon.jsx'
import { fmtMxn, fmtDateTime, trendOf } from '../utils/format.js'
import './TradeHistory.css'

function StatusBadge({ status, closeReason }) {
  if (status === 'OPEN') return <span className="tag tag-open">Abierta</span>
  const label = closeReason === 'STOP_LOSS' ? 'Stop loss'
              : closeReason === 'TAKE_PROFIT' ? 'Take profit'
              : 'Por señal'
  return <span className="tag">{label}</span>
}

function PnlCell({ trade }) {
  if (trade.status === 'OPEN') {
    const pnl = trade.unrealizedPnl
    return (
      <span className={`pnl pnl-${trendOf(pnl)}`}>
        {fmtMxn(pnl)} <small>no realizado</small>
      </span>
    )
  }
  const pnl = trade.profitLoss
  if (pnl == null) return <span>—</span>
  const pct = trade.profitLossPct || 0
  return (
    <span className={`pnl pnl-${trendOf(pnl)}`}>
      {pnl > 0 ? '+' : ''}{fmtMxn(pnl)} <small>{pct > 0 ? '+' : ''}{pct.toFixed(1)}%</small>
    </span>
  )
}

function TradeHistory({ trades }) {
  const items = trades || []

  return (
    <Panel aria-labelledby="trades-title">
      <PanelHead eyebrow="Últimas 20" title="Operaciones simuladas" id="trades-title" />

      {items.length === 0 ? (
        <EmptyState icon={<Icon name="wallet" size={22} />} title="Aún no hay operaciones">
          El bot abre una cuando detecta una señal de compra con 60% de confianza o más.
        </EmptyState>
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th scope="col">Estado</th>
                <th scope="col" className="num">Entrada</th>
                <th scope="col" className="num">Salida / actual</th>
                <th scope="col" className="num">Invertido</th>
                <th scope="col" className="num">BTC</th>
                <th scope="col" className="num">Stop loss</th>
                <th scope="col" className="num">Take profit</th>
                <th scope="col" className="num">P&amp;L</th>
                <th scope="col" className="num">Apertura</th>
              </tr>
            </thead>
            <tbody>
              {items.map((t) => (
                <tr key={t.id} className={t.status === 'OPEN' ? 'is-open' : ''}>
                  <td><StatusBadge status={t.status} closeReason={t.closeReason} /></td>
                  <td className="num">{fmtMxn(t.entryPrice)}</td>
                  <td className="num">{t.exitPrice ? fmtMxn(t.exitPrice) : fmtMxn(t.currentPrice)}</td>
                  <td className="num">{fmtMxn(t.investedMxn)}</td>
                  <td className="num">{t.quantity ? t.quantity.toFixed(6) : '—'}</td>
                  <td className="num muted">{fmtMxn(t.stopLoss)}</td>
                  <td className="num muted">{fmtMxn(t.takeProfit)}</td>
                  <td className="num"><PnlCell trade={t} /></td>
                  <td className="num muted">{fmtDateTime(t.openedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Panel>
  )
}

export default TradeHistory
