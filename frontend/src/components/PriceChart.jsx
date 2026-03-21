import ReactApexChart from 'react-apexcharts'
import { computeBollingerSeries } from '../utils/indicators.js'
import './PriceChart.css'

function PriceChart({ ohlcv }) {
  if (!ohlcv || ohlcv.length === 0) return null

  const candleData = ohlcv.map(c => ({
    x: new Date(c.timestamp * 1000),
    y: [
      parseFloat(c.open.toFixed(2)),
      parseFloat(c.high.toFixed(2)),
      parseFloat(c.low.toFixed(2)),
      parseFloat(c.close.toFixed(2)),
    ],
  }))

  const { upper, middle, lower } = computeBollingerSeries(ohlcv, 20)

  const series = [
    {
      name: 'BTC/MXN',
      type: 'candlestick',
      data: candleData,
    },
    {
      name: 'BB Superior',
      type: 'line',
      data: upper,
    },
    {
      name: 'BB Media',
      type: 'line',
      data: middle,
    },
    {
      name: 'BB Inferior',
      type: 'line',
      data: lower,
    },
  ]

  const options = {
    chart: {
      type: 'candlestick',
      height: 420,
      background: '#0f1117',
      toolbar: {
        show: true,
        tools: {
          download: true,
          selection: true,
          zoom: true,
          zoomin: true,
          zoomout: true,
          pan: true,
          reset: true,
        },
      },
      zoom: { enabled: true },
      animations: { enabled: false },
    },
    title: {
      text: 'Precio de Bitcoin — Últimas 5 días (velas de 1 hora)',
      align: 'left',
      style: { color: '#e5e7eb', fontSize: '14px', fontWeight: '600' },
    },
    xaxis: {
      type: 'datetime',
      labels: {
        style: { colors: '#6b7280', fontSize: '11px' },
        datetimeFormatter: {
          year: 'yyyy',
          month: "MMM 'yy",
          day: 'dd MMM',
          hour: 'HH:mm',
        },
      },
      axisBorder: { color: '#2d3148' },
      axisTicks: { color: '#2d3148' },
    },
    yaxis: {
      tooltip: { enabled: true },
      labels: {
        style: { colors: '#6b7280', fontSize: '11px' },
        formatter: (val) => `$${val.toLocaleString('es-MX')}`,
      },
    },
    grid: {
      borderColor: '#1e2130',
      strokeDashArray: 3,
    },
    plotOptions: {
      candlestick: {
        colors: {
          upward: '#22c55e',
          downward: '#ef4444',
        },
        wick: {
          useFillColor: true,
        },
      },
    },
    stroke: {
      width: [1, 1.5, 1.5, 1.5],
      dashArray: [0, 0, 4, 0],
    },
    colors: ['transparent', '#3b82f6', '#8b8fa8', '#3b82f6'],
    legend: {
      show: true,
      labels: { colors: '#6b7280' },
    },
    tooltip: {
      theme: 'dark',
      x: { format: 'dd MMM HH:mm' },
    },
    theme: { mode: 'dark' },
  }

  return (
    <div className="price-chart card">
      <ReactApexChart
        options={options}
        series={series}
        type="candlestick"
        height={420}
      />
      <div className="chart-info">
        <span className="info-item">
          <span className="info-dot green"></span> Vela verde = precio subió
        </span>
        <span className="info-item">
          <span className="info-dot red"></span> Vela roja = precio bajó
        </span>
        <span className="info-item">
          <span className="info-dot blue"></span> Bandas de Bollinger (BB)
        </span>
      </div>
    </div>
  )
}

export default PriceChart
