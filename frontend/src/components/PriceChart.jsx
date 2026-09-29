import ReactApexChart from 'react-apexcharts'
import { computeBollingerSeries } from '../utils/indicators.js'
import { CHART, baseChart, baseGrid, axisLabels } from '../utils/chartTheme.js'
import { Panel, PanelHead, EmptyState } from './ui/Panel.jsx'
import Icon from './ui/Icon.jsx'
import './PriceChart.css'

function PriceChart({ ohlcv }) {
  if (!ohlcv || ohlcv.length === 0) {
    return (
      <Panel aria-labelledby="price-chart-title">
        <PanelHead eyebrow="BTC/MXN · velas de 1 h" title="Precio" id="price-chart-title" />
        <EmptyState icon={<Icon name="chart" size={22} />} title="Sin velas por ahora">
          Bitso no devolvió datos OHLCV. Se vuelve a intentar cada 5 minutos.
        </EmptyState>
      </Panel>
    )
  }

  const candleData = ohlcv.map((c) => ({
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
    { name: 'BTC/MXN', type: 'candlestick', data: candleData },
    { name: 'Banda superior', type: 'line', data: upper },
    { name: 'Media (SMA 20)', type: 'line', data: middle },
    { name: 'Banda inferior', type: 'line', data: lower },
  ]

  const options = {
    chart: baseChart({
      type: 'candlestick',
      height: 400,
      zoom: { enabled: true },
      toolbar: {
        show: true,
        tools: { download: false, selection: true, zoom: true, zoomin: true, zoomout: true, pan: true, reset: true },
      },
    }),
    xaxis: {
      type: 'datetime',
      labels: {
        ...axisLabels,
        datetimeUTC: false,
        datetimeFormatter: { year: 'yyyy', month: "MMM 'yy", day: 'dd MMM', hour: 'HH:mm' },
      },
      axisBorder: { color: CHART.axis },
      axisTicks: { color: CHART.axis },
    },
    yaxis: {
      tooltip: { enabled: true },
      labels: { ...axisLabels, formatter: (val) => `$${Math.round(val).toLocaleString('es-MX')}` },
    },
    grid: baseGrid,
    plotOptions: {
      candlestick: {
        colors: { upward: CHART.up, downward: CHART.down },
        wick: { useFillColor: true },
      },
    },
    stroke: { width: [1, 1.5, 1.5, 1.5], dashArray: [0, 0, 4, 0] },
    colors: [CHART.text, CHART.series1, CHART.series1, CHART.series1],
    // La leyenda va en HTML debajo de la gráfica; la de Apex choca con la barra de herramientas
    legend: { show: false },
    tooltip: { theme: 'dark', shared: true, x: { format: 'dd MMM HH:mm' } },
    theme: { mode: 'dark' },
  }

  return (
    <Panel className="chart-panel" aria-labelledby="price-chart-title">
      <PanelHead eyebrow="BTC/MXN · velas de 1 h" title="Precio con bandas de Bollinger" id="price-chart-title" />
      <div className="chart-wrap">
        <ReactApexChart options={options} series={series} type="candlestick" height={400} />
      </div>
      <ul className="chart-key">
        <li><span className="key-swatch key-up" /> Vela que cerró arriba de su apertura</li>
        <li><span className="key-swatch key-down" /> Vela que cerró abajo</li>
        <li><span className="key-swatch key-band" /> Bandas superior e inferior (± 2 desviaciones)</li>
        <li><span className="key-swatch key-band key-dashed" /> Media de 20 h</li>
      </ul>
    </Panel>
  )
}

export default PriceChart
