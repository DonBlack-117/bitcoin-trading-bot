import { Panel } from './ui/Panel.jsx'
import Delta from './ui/Delta.jsx'
import { fmtMxn, fmtBtc, fmtNum, trendOf } from '../utils/format.js'
import './Portfolio.css'

function Portfolio({ portfolio }) {
  if (!portfolio) return null

  const {
    initialCapital, mxnBalance, btcBalance, totalValueMxn,
    unrealizedPnl, totalReturn, totalReturnPct,
    totalTrades, winningTrades, winRate,
  } = portfolio

  const cashShare = totalValueMxn ? Math.min(100, Math.max(0, (mxnBalance / totalValueMxn) * 100)) : 100

  return (
    <div className="portfolio-grid">
      <Panel className="portfolio-total" aria-label="Valor del portafolio">
        <p className="eyebrow">Valor total</p>
        <p className="portfolio-value">{fmtMxn(totalValueMxn)}</p>
        <div className="portfolio-return">
          <span className={`pnl pnl-${trendOf(totalReturn)}`}>
            {totalReturn > 0 ? '+' : ''}{fmtMxn(totalReturn)}
          </span>
          <Delta value={totalReturnPct} />
          <span className="portfolio-muted">desde {fmtMxn(initialCapital, 0)} iniciales</span>
        </div>

        <div className="alloc">
          <div className="alloc-bar" role="img" aria-label={`${fmtNum(cashShare, 0)}% en pesos y ${fmtNum(100 - cashShare, 0)}% en BTC`}>
            <span className="alloc-cash" style={{ width: `${cashShare}%` }} />
            <span className="alloc-btc" />
          </div>
          <dl className="alloc-legend">
            <div>
              <dt><span className="alloc-swatch is-cash" /> Pesos disponibles</dt>
              <dd>{fmtMxn(mxnBalance)}</dd>
            </div>
            <div>
              <dt><span className="alloc-swatch is-btc" /> BTC en cartera</dt>
              <dd>{fmtBtc(btcBalance)}</dd>
            </div>
          </dl>
        </div>
      </Panel>

      <Panel className="portfolio-stats" aria-label="Resultados de las operaciones">
        <div className="stat">
          <span className="stat-label">P&amp;L no realizado</span>
          <span className={`stat-value pnl pnl-${trendOf(unrealizedPnl)}`}>{fmtMxn(unrealizedPnl)}</span>
          <span className="stat-sub">De la operación abierta</span>
        </div>
        <div className="stat">
          <span className="stat-label">Tasa de éxito</span>
          <span className="stat-value">{winRate != null ? `${fmtNum(winRate, 1)}%` : '—'}</span>
          <div className="winrate" aria-hidden="true">
            <span style={{ transform: `scaleX(${Math.min(100, winRate || 0) / 100})` }} />
          </div>
        </div>
        <div className="stat">
          <span className="stat-label">Operaciones cerradas</span>
          <span className="stat-value">{totalTrades ?? 0}</span>
          <span className="stat-sub">{winningTrades ?? 0} con ganancia</span>
        </div>
      </Panel>
    </div>
  )
}

export default Portfolio
