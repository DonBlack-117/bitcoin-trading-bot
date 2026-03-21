import './TradeHistory.css'

function fmtMxn(n) {
  if (n == null) return '—'
  return '$' + n.toLocaleString('es-MX', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function fmtDate(str) {
  if (!str) return '—'
  return new Date(str).toLocaleString('es-MX', {
    month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

function StatusBadge({ status, closeReason }) {
  if (status === 'OPEN') return <span className="trade-badge trade-open">ABIERTA</span>
  const label = closeReason === 'STOP_LOSS' ? 'STOP LOSS'
              : closeReason === 'TAKE_PROFIT' ? 'TAKE PROFIT'
              : 'SEÑAL'
  return <span className="trade-badge trade-closed">{label}</span>
}

function PnlCell({ trade }) {
  if (trade.status === 'OPEN') {
    const pnl = trade.unrealizedPnl
    const cls = pnl >= 0 ? 'pnl-positive' : 'pnl-negative'
    return <span className={cls}>{fmtMxn(pnl)} <small>(no realizado)</small></span>
  }
  const pnl = trade.profitLoss
  if (pnl == null) return <span>—</span>
  const cls = pnl >= 0 ? 'pnl-positive' : 'pnl-negative'
  const sign = pnl >= 0 ? '+' : ''
  return <span className={cls}>{fmtMxn(pnl)} ({sign}{(trade.profitLossPct || 0).toFixed(1)}%)</span>
}

function TradeHistory({ trades }) {
  if (!trades || trades.length === 0) {
    return (
      <div className="trade-history card">
        <h2 className="trade-history-title">Operaciones Simuladas</h2>
        <p className="trade-empty">Aún no hay operaciones. El bot abrirá una cuando detecte una señal de compra con ≥60% confianza.</p>
      </div>
    )
  }

  return (
    <div className="trade-history card">
      <h2 className="trade-history-title">Operaciones Simuladas</h2>
      <div className="trade-table-wrap">
        <table className="trade-table">
          <thead>
            <tr>
              <th>Estado</th>
              <th>Entrada</th>
              <th>Salida</th>
              <th>Invertido</th>
              <th>BTC</th>
              <th>Stop Loss</th>
              <th>Take Profit</th>
              <th>P&amp;L</th>
              <th>Apertura</th>
            </tr>
          </thead>
          <tbody>
            {trades.map(t => (
              <tr key={t.id} className={t.status === 'OPEN' ? 'row-open' : ''}>
                <td><StatusBadge status={t.status} closeReason={t.closeReason} /></td>
                <td>{fmtMxn(t.entryPrice)}</td>
                <td>{t.exitPrice ? fmtMxn(t.exitPrice) : fmtMxn(t.currentPrice)}</td>
                <td>{fmtMxn(t.investedMxn)}</td>
                <td>{t.quantity ? t.quantity.toFixed(6) : '—'}</td>
                <td className="pnl-negative">{fmtMxn(t.stopLoss)}</td>
                <td className="pnl-positive">{fmtMxn(t.takeProfit)}</td>
                <td><PnlCell trade={t} /></td>
                <td>{fmtDate(t.openedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default TradeHistory
