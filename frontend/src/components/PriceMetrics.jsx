import { Panel, PanelHead } from './ui/Panel.jsx'
import Delta from './ui/Delta.jsx'
import { fmtMxn, fmtBtc, fmtNum } from '../utils/format.js'
import './PriceMetrics.css'

/** Barra con soporte, VWAP, resistencia y la posición del precio actual. */
function KeyLevels({ price, support, vwap, resistance }) {
  if (!price || !support || !resistance) return null

  const min = Math.min(support, price, vwap || price)
  const max = Math.max(resistance, price, vwap || price)
  const span = max - min || 1
  const pos = (v) => `${(((v - min) / span) * 100).toFixed(2)}%`

  return (
    <div className="levels">
      <div className="levels-head">
        <span className="stat-label">Niveles clave</span>
        <span className="levels-note">Distancia al soporte {fmtNum(((price - support) / price) * 100, 1)}%</span>
      </div>
      <div className="levels-track" role="img" aria-label={`Precio ${fmtMxn(price, 0)} entre soporte ${fmtMxn(support, 0)} y resistencia ${fmtMxn(resistance, 0)}`}>
        <span className="levels-fill" style={{ left: pos(support), right: `calc(100% - ${pos(resistance)})` }} />
        {vwap ? <span className="levels-tick levels-vwap" style={{ left: pos(vwap) }} /> : null}
        <span className="levels-price" style={{ left: pos(price) }} />
      </div>
      <dl className="levels-legend">
        <div><dt>Soporte</dt><dd>{fmtMxn(support, 0)}</dd></div>
        {vwap ? <div><dt>VWAP</dt><dd>{fmtMxn(vwap, 0)}</dd></div> : null}
        <div><dt>Resistencia</dt><dd>{fmtMxn(resistance, 0)}</dd></div>
      </dl>
    </div>
  )
}

function PriceMetrics({ ticker, indicators }) {
  if (!ticker) return null

  const { last, ask, bid, volume, change24h } = ticker
  const btcPer100 = last > 0 ? 100 / last : 0
  const spread = ask && bid ? ask - bid : null

  return (
    <Panel className="price-panel" aria-labelledby="price-title">
      <PanelHead eyebrow="Precio actual" title="Bitcoin en pesos" id="price-title">
        <Delta value={change24h} suffix="24 h" className="delta-chip" />
      </PanelHead>

      <p className="price-hero">
        <span className="price-currency">MXN</span>
        {fmtNum(last, 2)}
      </p>

      <div className="price-stats">
        <div className="stat">
          <span className="stat-label">Compra (ask)</span>
          <span className="stat-value">{fmtMxn(ask)}</span>
          <span className="stat-sub">Lo que pagas por 1 BTC</span>
        </div>
        <div className="stat">
          <span className="stat-label">Venta (bid)</span>
          <span className="stat-value">{fmtMxn(bid)}</span>
          <span className="stat-sub">Lo que te pagan por 1 BTC</span>
        </div>
        <div className="stat">
          <span className="stat-label">Diferencial</span>
          <span className="stat-value">{fmtMxn(spread)}</span>
          <span className="stat-sub">Entre compra y venta</span>
        </div>
        <div className="stat">
          <span className="stat-label">Con $100 MXN</span>
          <span className="stat-value">{fmtBtc(btcPer100)}</span>
          <span className="stat-sub">BTC aproximados</span>
        </div>
      </div>

      <KeyLevels
        price={last}
        support={indicators?.support}
        vwap={indicators?.vwap}
        resistance={indicators?.resistance}
      />

      {volume ? <p className="price-foot">Volumen 24 h: <span>{fmtNum(volume, 2)} BTC</span></p> : null}
    </Panel>
  )
}

export default PriceMetrics
