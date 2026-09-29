import ReactApexChart from 'react-apexcharts'
import { computeRsiSeries } from '../utils/indicators.js'
import { CHART, baseChart, baseGrid, axisLabels } from '../utils/chartTheme.js'
import { Panel, PanelHead } from './ui/Panel.jsx'
import './RSIChart.css'

function RSIChart({ ohlcv }) {
  if (!ohlcv || ohlcv.length < 15) return null

  const rsiData = computeRsiSeries(ohlcv, 14)
  const last = rsiData.length ? rsiData[rsiData.length - 1].y : null

  const threshold = (y, color, text) => ({
    y,
    borderColor: color,
    borderWidth: 1,
    strokeDashArray: 4,
    label: {
      text,
      position: 'left',
      textAnchor: 'start',
      offsetX: 8,
      borderWidth: 0,
      style: { color: CHART.text, background: 'transparent', fontSize: '11px', fontFamily: CHART.font },
    },
  })

  const options = {
    chart: baseChart({ type: 'line', height: 200 }),
    xaxis: {
      type: 'datetime',
      labels: { ...axisLabels, datetimeUTC: false, datetimeFormatter: { hour: 'HH:mm', day: 'dd MMM' } },
      axisBorder: { color: CHART.axis },
      axisTicks: { color: CHART.axis },
      tooltip: { enabled: false },
    },
    yaxis: {
      min: 0,
      max: 100,
      tickAmount: 4,
      labels: { ...axisLabels, formatter: (val) => val.toFixed(0) },
    },
    grid: baseGrid,
    stroke: { width: 2, curve: 'smooth' },
    colors: [CHART.series1],
    annotations: {
      yaxis: [
        threshold(70, CHART.down, 'Sobrecompra (70)'),
        threshold(30, CHART.up, 'Sobreventa (30)'),
      ],
    },
    tooltip: {
      theme: 'dark',
      x: { format: 'dd MMM HH:mm' },
      y: { formatter: (val) => val?.toFixed(2), title: { formatter: () => 'RSI' } },
    },
    legend: { show: false },
    theme: { mode: 'dark' },
  }

  return (
    <Panel className="rsi-panel" aria-labelledby="rsi-chart-title">
      <PanelHead eyebrow="Índice de fuerza relativa · 14 periodos" title="RSI" id="rsi-chart-title">
        {last != null && <span className="rsi-last">{last.toFixed(1)}</span>}
      </PanelHead>
      <div className="chart-wrap">
        <ReactApexChart options={options} series={[{ name: 'RSI', data: rsiData }]} type="line" height={200} />
      </div>
      <p className="rsi-help">Arriba de 70 suele anticipar una caída (posible venta). Abajo de 30, un rebote (posible compra).</p>
    </Panel>
  )
}

export default RSIChart
