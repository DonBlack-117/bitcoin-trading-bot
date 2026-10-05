import ReactApexChart from 'react-apexcharts'
import { fmtUsd, fmtPct, fmtLargeUsd, trendOf } from '../utils/format.js'
import { CHART, baseChart, baseGrid, axisLabels } from '../utils/chartTheme.js'
import { Panel, PanelHead, EmptyState } from './ui/Panel.jsx'
import Icon from './ui/Icon.jsx'
import Delta from './ui/Delta.jsx'
import './GlobalMarket.css'

function fearGreedTone(score) {
  if (score < 25) return 'down'
  if (score < 45) return 'serious'
  if (score < 55) return 'hold'
  return 'up'
}

function MoverList({ title, items }) {
  return (
    <div className="movers">
      <h4 className="movers-title">{title}</h4>
      <ol className="movers-list">
        {items.map((c) => (
          <li key={c.symbol} className="movers-item">
            <span className="movers-rank">{c.rank}</span>
            <span className="movers-name">
              <b>{c.symbol}</b>
              <span>{c.name}</span>
            </span>
            <Delta value={c.pct24h} />
          </li>
        ))}
      </ol>
    </div>
  )
}

function GlobalMarket({ market }) {
  if (!market) {
    return (
      <Panel aria-label="Mercado global">
        <EmptyState icon={<Icon name="globe" size={22} />} title="Sin datos del mercado global">
          Revisa que el backend tenga la variable <code>CMC_API_KEY</code>. El resto del dashboard funciona sin ella.
        </EmptyState>
      </Panel>
    )
  }

  const {
    totalMarketCap, totalVolume24h, btcDominance, activeCryptos,
    fearGreedScore, fearGreedLabel, topCryptos = [],
    ethDominance, marketCapChange24h,
  } = market

  const othersPercent = Math.max(0, 100 - btcDominance - ethDominance)
  const fgTone = fearGreedTone(fearGreedScore)

  const donutOptions = {
    chart: baseChart({ type: 'donut' }),
    labels: ['Bitcoin', 'Ethereum', 'Otras'],
    colors: [CHART.btc, CHART.series1, CHART.other],
    stroke: { width: 2, colors: ['#161615'] },
    legend: { show: false },
    dataLabels: { enabled: false },
    tooltip: { theme: 'dark', y: { formatter: (v) => `${v.toFixed(1)}%` } },
    plotOptions: {
      pie: {
        donut: {
          size: '72%',
          labels: {
            show: true,
            name: { color: CHART.text, fontFamily: CHART.font, fontSize: '12px', offsetY: 18 },
            value: { color: '#f2f1ec', fontFamily: CHART.mono, fontSize: '22px', offsetY: -12, formatter: (v) => `${parseFloat(v).toFixed(1)}%` },
            total: { show: true, label: 'Bitcoin', color: CHART.text, fontFamily: CHART.font, formatter: () => `${btcDominance.toFixed(1)}%` },
          },
        },
      },
    },
    theme: { mode: 'dark' },
  }

  const donutSeries = [
    parseFloat(btcDominance.toFixed(1)),
    parseFloat(ethDominance.toFixed(1)),
    parseFloat(othersPercent.toFixed(1)),
  ]

  const top15 = topCryptos.slice(0, 15)
  const barOptions = {
    chart: baseChart({ type: 'bar' }),
    plotOptions: {
      bar: {
        borderRadius: 3,
        borderRadiusApplication: 'end',
        columnWidth: '58%',
        colors: {
          ranges: [
            { from: -100, to: 0, color: CHART.down },
            { from: 0, to: 100, color: CHART.up },
          ],
        },
      },
    },
    dataLabels: { enabled: false },
    xaxis: {
      categories: top15.map((c) => c.symbol),
      labels: axisLabels,
      axisBorder: { color: CHART.axis },
      axisTicks: { show: false },
    },
    yaxis: { labels: { ...axisLabels, formatter: (v) => `${v.toFixed(1)}%` } },
    grid: baseGrid,
    legend: { show: false },
    tooltip: { theme: 'dark', y: { formatter: (v) => fmtPct(v) } },
    theme: { mode: 'dark' },
  }

  const barSeries = [{ name: 'Cambio 24 h', data: top15.map((c) => parseFloat((c.pct24h || 0).toFixed(2))) }]

  const sorted = [...topCryptos].sort((a, b) => (b.pct24h || 0) - (a.pct24h || 0))
  const gainers = sorted.slice(0, 3)
  const losers = sorted.slice(-3).reverse()

  return (
    <div className="market">
      <div className="market-stats">
        <Panel className="market-cap">
          <p className="eyebrow">Capitalización total</p>
          <p className="market-cap-value">{fmtLargeUsd(totalMarketCap)}</p>
          <Delta value={marketCapChange24h} suffix="24 h" />
        </Panel>
        <Panel>
          <div className="stat">
            <span className="stat-label">Volumen 24 h</span>
            <span className="stat-value">{fmtLargeUsd(totalVolume24h)}</span>
          </div>
        </Panel>
        <Panel>
          <div className="stat">
            <span className="stat-label">Criptomonedas activas</span>
            <span className="stat-value">{activeCryptos.toLocaleString('es-MX')}</span>
          </div>
        </Panel>
        <Panel className={`fear-greed fg-${fgTone}`}>
          <div className="stat">
            <span className="stat-label">Miedo y codicia</span>
            <span className="stat-value">
              {fearGreedScore} <span className="fg-label">{fearGreedLabel}</span>
            </span>
          </div>
          <div className="fg-track" role="img" aria-label={`Índice ${fearGreedScore} de 100: ${fearGreedLabel}`}>
            <span className="fg-marker" style={{ left: `${Math.min(100, Math.max(0, fearGreedScore))}%` }} />
          </div>
        </Panel>
      </div>

      <div className="bento">
        <Panel className="span-4" aria-labelledby="dominance-title">
          <PanelHead eyebrow="Participación en la capitalización" title="Dominancia" id="dominance-title" />
          <ReactApexChart options={donutOptions} series={donutSeries} type="donut" height={240} />
          <ul className="dominance-key">
            <li><span className="key-dot is-btc" /> Bitcoin <b>{btcDominance.toFixed(1)}%</b></li>
            <li><span className="key-dot is-eth" /> Ethereum <b>{ethDominance.toFixed(1)}%</b></li>
            <li><span className="key-dot is-other" /> Otras <b>{othersPercent.toFixed(1)}%</b></li>
          </ul>
        </Panel>

        <Panel className="span-8" aria-labelledby="perf-title">
          <PanelHead eyebrow="Top 15 por capitalización" title="Rendimiento en 24 h" id="perf-title" />
          <ReactApexChart options={barOptions} series={barSeries} type="bar" height={210} />
          <div className="movers-row">
            <MoverList title="Suben más" items={gainers} />
            <MoverList title="Bajan más" items={losers} />
          </div>
        </Panel>
      </div>

      <Panel aria-labelledby="top20-title">
        <PanelHead eyebrow="Precios en USD" title="Top 20 criptomonedas" id="top20-title" />
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th scope="col" className="num">#</th>
                <th scope="col">Nombre</th>
                <th scope="col" className="num">Precio</th>
                <th scope="col" className="num">Cap. de mercado</th>
                <th scope="col" className="num">Volumen 24 h</th>
                <th scope="col" className="num">1 h</th>
                <th scope="col" className="num">24 h</th>
                <th scope="col" className="num">7 d</th>
              </tr>
            </thead>
            <tbody>
              {topCryptos.map((c) => (
                <tr key={c.symbol}>
                  <td className="num muted">{c.rank}</td>
                  <td>
                    <span className="coin">
                      <b>{c.symbol}</b>
                      <span>{c.name}</span>
                    </span>
                  </td>
                  <td className="num">{fmtUsd(c.price)}</td>
                  <td className="num">{fmtLargeUsd(c.marketCap)}</td>
                  <td className="num">{fmtLargeUsd(c.volume24h)}</td>
                  <td className={`num pnl pnl-${trendOf(c.pct1h)}`}>{fmtPct(c.pct1h)}</td>
                  <td className={`num pnl pnl-${trendOf(c.pct24h)}`}>{fmtPct(c.pct24h)}</td>
                  <td className={`num pnl pnl-${trendOf(c.pct7d)}`}>{fmtPct(c.pct7d)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Panel>
    </div>
  )
}

export default GlobalMarket
