import ReactApexChart from 'react-apexcharts'
import { formatLargeNumber, formatPct } from '../utils/indicators.js'
import './GlobalMarket.css'

function MetricCard({ label, value, sub, color }) {
  return (
    <div className="gm-metric-card">
      <div className="gm-metric-label">{label}</div>
      <div className="gm-metric-value" style={color ? { color } : {}}>
        {value}
      </div>
      {sub && <div className="gm-metric-sub">{sub}</div>}
    </div>
  )
}

function GlobalMarket({ market }) {
  if (!market) return null

  const {
    totalMarketCap,
    totalVolume24h,
    btcDominance,
    activeCryptos,
    fearGreedScore,
    fearGreedLabel,
    fearGreedColor,
    topCryptos = [],
    ethDominance,
    marketCapChange24h,
  } = market

  const othersPercent = Math.max(0, 100 - btcDominance - ethDominance)

  const donutOptions = {
    chart: { type: 'donut', background: '#0f1117' },
    labels: ['Bitcoin (BTC)', 'Ethereum (ETH)', 'Otros'],
    colors: ['#f59e0b', '#3b82f6', '#6b7280'],
    legend: {
      position: 'bottom',
      labels: { colors: '#6b7280', fontSize: '12px' },
    },
    dataLabels: { style: { fontSize: '12px' } },
    tooltip: { theme: 'dark', y: { formatter: (v) => `${v.toFixed(1)}%` } },
    theme: { mode: 'dark' },
    plotOptions: { pie: { donut: { size: '60%' } } },
  }

  const donutSeries = [
    parseFloat(btcDominance.toFixed(1)),
    parseFloat(ethDominance.toFixed(1)),
    parseFloat(othersPercent.toFixed(1)),
  ]

  // Top 15 for bar chart
  const top15 = topCryptos.slice(0, 15)
  const barOptions = {
    chart: {
      type: 'bar',
      background: '#0f1117',
      toolbar: { show: false },
      animations: { enabled: false },
    },
    title: {
      text: 'Rendimiento 24h — Top 15',
      align: 'left',
      style: { color: '#e5e7eb', fontSize: '13px' },
    },
    plotOptions: {
      bar: {
        horizontal: false,
        borderRadius: 4,
        distributed: true,
        colors: {
          ranges: [
            { from: -100, to: 0, color: '#ef4444' },
            { from: 0, to: 100, color: '#22c55e' },
          ],
        },
      },
    },
    xaxis: {
      categories: top15.map(c => c.symbol),
      labels: { style: { colors: '#6b7280', fontSize: '10px' } },
      axisBorder: { color: '#2d3148' },
      axisTicks: { color: '#2d3148' },
    },
    yaxis: {
      labels: {
        style: { colors: '#6b7280', fontSize: '10px' },
        formatter: (v) => `${v.toFixed(1)}%`,
      },
    },
    grid: { borderColor: '#1e2130', strokeDashArray: 3 },
    legend: { show: false },
    tooltip: {
      theme: 'dark',
      y: { formatter: (v) => `${v?.toFixed(2)}%` },
    },
    theme: { mode: 'dark' },
  }

  const barSeries = [
    {
      name: 'Cambio 24h',
      data: top15.map(c => parseFloat((c.pct24h || 0).toFixed(2))),
    },
  ]

  // Gainers / Losers
  const sorted = [...topCryptos].sort((a, b) => (b.pct24h || 0) - (a.pct24h || 0))
  const gainers = sorted.slice(0, 3)
  const losers = sorted.slice(-3).reverse()

  const mcChange = marketCapChange24h >= 0
    ? `+${marketCapChange24h.toFixed(2)}%`
    : `${marketCapChange24h.toFixed(2)}%`

  return (
    <div className="global-market">
      <h2 className="gm-title">Mercado Global de Criptomonedas</h2>

      <div className="gm-metrics-row">
        <MetricCard
          label="Capitalización Total"
          value={formatLargeNumber(totalMarketCap)}
          sub={`Cambio 24h: ${mcChange}`}
        />
        <MetricCard
          label="Volumen 24h"
          value={formatLargeNumber(totalVolume24h)}
        />
        <MetricCard
          label="Dominancia BTC"
          value={`${btcDominance.toFixed(1)}%`}
          sub={`ETH: ${ethDominance.toFixed(1)}%`}
        />
        <MetricCard
          label="Criptomonedas Activas"
          value={activeCryptos.toLocaleString()}
        />
        <MetricCard
          label="Fear & Greed"
          value={`${fearGreedScore} — ${fearGreedLabel}`}
          color={fearGreedColor}
        />
      </div>

      <div className="gm-charts-row">
        <div className="gm-donut card">
          <h3 className="gm-subtitle">Dominancia del Mercado</h3>
          <ReactApexChart
            options={donutOptions}
            series={donutSeries}
            type="donut"
            height={260}
          />
        </div>

        <div className="gm-gainers-losers card">
          <div className="gm-group">
            <h3 className="gm-subtitle gm-green">Top 3 Ganadoras</h3>
            <ul className="gm-crypto-list">
              {gainers.map(c => (
                <li key={c.symbol} className="gm-crypto-item">
                  <span className="gm-crypto-rank">#{c.rank}</span>
                  <span className="gm-crypto-symbol">{c.symbol}</span>
                  <span className="gm-crypto-name">{c.name}</span>
                  <span className="gm-pct gm-green">{formatPct(c.pct24h)}</span>
                </li>
              ))}
            </ul>
          </div>
          <div className="gm-group">
            <h3 className="gm-subtitle gm-red">Top 3 Perdedoras</h3>
            <ul className="gm-crypto-list">
              {losers.map(c => (
                <li key={c.symbol} className="gm-crypto-item">
                  <span className="gm-crypto-rank">#{c.rank}</span>
                  <span className="gm-crypto-symbol">{c.symbol}</span>
                  <span className="gm-crypto-name">{c.name}</span>
                  <span className="gm-pct gm-red">{formatPct(c.pct24h)}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>

      <div className="gm-bar-card card">
        <ReactApexChart
          options={barOptions}
          series={barSeries}
          type="bar"
          height={240}
        />
      </div>

      <div className="gm-table-card card">
        <h3 className="gm-subtitle">Top 20 Criptomonedas</h3>
        <div className="gm-table-wrapper">
          <table className="gm-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Nombre</th>
                <th>Precio (USD)</th>
                <th>Cap. Mercado</th>
                <th>Volumen 24h</th>
                <th>1h %</th>
                <th>24h %</th>
                <th>7d %</th>
              </tr>
            </thead>
            <tbody>
              {topCryptos.map(c => (
                <tr key={c.symbol}>
                  <td className="gm-td-rank">{c.rank}</td>
                  <td className="gm-td-name">
                    <span className="gm-sym">{c.symbol}</span>
                    <span className="gm-fullname">{c.name}</span>
                  </td>
                  <td className="gm-td-num">
                    {c.price >= 1
                      ? `$${c.price.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
                      : `$${c.price.toFixed(6)}`}
                  </td>
                  <td className="gm-td-num">{formatLargeNumber(c.marketCap)}</td>
                  <td className="gm-td-num">{formatLargeNumber(c.volume24h)}</td>
                  <td className={getPctClass(c.pct1h)}>{formatPct(c.pct1h)}</td>
                  <td className={getPctClass(c.pct24h)}>{formatPct(c.pct24h)}</td>
                  <td className={getPctClass(c.pct7d)}>{formatPct(c.pct7d)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

function getPctClass(val) {
  if (val === null || val === undefined) return 'gm-td-num'
  if (val > 0) return 'gm-td-num gm-green'
  if (val < 0) return 'gm-td-num gm-red'
  return 'gm-td-num'
}

export default GlobalMarket
