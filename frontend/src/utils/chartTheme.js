// Colores de las gráficas (ApexCharts no lee variables CSS).
// Paleta validada con el validador de dataviz en modo oscuro sobre #161615.
export const CHART = {
  surface: 'transparent',
  grid: 'rgba(255, 255, 240, 0.06)',
  axis: 'rgba(255, 255, 240, 0.10)',
  label: '#7d7c75',
  text: '#b9b8b0',
  up: '#0ca30c',
  down: '#d03b3b',
  warning: '#fab219',
  series1: '#3987e5',
  btc: '#c98500',
  other: '#5b5a55',
  font: "'Geist', system-ui, sans-serif",
  mono: "'Geist Mono', ui-monospace, monospace",
}

/** Opciones comunes para todas las gráficas. */
export function baseChart(overrides = {}) {
  return {
    background: CHART.surface,
    fontFamily: CHART.font,
    foreColor: CHART.label,
    toolbar: { show: false },
    animations: { enabled: false },
    ...overrides,
  }
}

export const baseGrid = {
  borderColor: CHART.grid,
  strokeDashArray: 3,
  padding: { left: 8, right: 8 },
}

export const axisLabels = {
  style: { colors: CHART.label, fontSize: '11px', fontFamily: CHART.mono },
}
