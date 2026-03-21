import ReactApexChart from 'react-apexcharts'
import { computeRsiSeries } from '../utils/indicators.js'
import './RSIChart.css'

function RSIChart({ ohlcv }) {
  if (!ohlcv || ohlcv.length < 15) return null

  const rsiData = computeRsiSeries(ohlcv, 14)

  const series = [
    {
      name: 'RSI',
      data: rsiData,
    },
  ]

  const options = {
    chart: {
      type: 'line',
      height: 220,
      background: '#0f1117',
      toolbar: { show: false },
      animations: { enabled: false },
    },
    title: {
      text: 'Indicador de Fuerza (RSI)',
      align: 'left',
      style: { color: '#e5e7eb', fontSize: '14px', fontWeight: '600' },
    },
    xaxis: {
      type: 'datetime',
      labels: {
        style: { colors: '#6b7280', fontSize: '11px' },
        datetimeFormatter: { hour: 'HH:mm', day: 'dd MMM' },
      },
      axisBorder: { color: '#2d3148' },
      axisTicks: { color: '#2d3148' },
    },
    yaxis: {
      min: 0,
      max: 100,
      tickAmount: 4,
      labels: {
        style: { colors: '#6b7280', fontSize: '11px' },
        formatter: (val) => val.toFixed(0),
      },
    },
    grid: {
      borderColor: '#1e2130',
      strokeDashArray: 3,
    },
    stroke: {
      width: 2,
      curve: 'smooth',
    },
    colors: ['#f59e0b'],
    annotations: {
      yaxis: [
        {
          y: 70,
          borderColor: '#ef4444',
          borderWidth: 1,
          strokeDashArray: 4,
          label: {
            text: 'Sobrecomprado (70)',
            style: { color: '#ef4444', background: 'transparent', fontSize: '11px' },
            position: 'right',
            offsetX: -10,
          },
        },
        {
          y: 30,
          borderColor: '#22c55e',
          borderWidth: 1,
          strokeDashArray: 4,
          label: {
            text: 'Sobrevendido (30)',
            style: { color: '#22c55e', background: 'transparent', fontSize: '11px' },
            position: 'right',
            offsetX: -10,
          },
        },
      ],
    },
    tooltip: {
      theme: 'dark',
      x: { format: 'dd MMM HH:mm' },
      y: { formatter: (val) => `RSI: ${val?.toFixed(2)}` },
    },
    theme: { mode: 'dark' },
  }

  return (
    <div className="rsi-chart card">
      <ReactApexChart
        options={options}
        series={series}
        type="line"
        height={220}
      />
      <div className="rsi-legend">
        <span className="rsi-legend-item rsi-sell">RSI &gt; 70: Sobrecomprado (posible venta)</span>
        <span className="rsi-legend-item rsi-buy">RSI &lt; 30: Sobrevendido (posible compra)</span>
      </div>
    </div>
  )
}

export default RSIChart
