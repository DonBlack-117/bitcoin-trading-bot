import { useState, useEffect, useCallback } from 'react'
import Header from './components/Header.jsx'
import SignalCard from './components/SignalCard.jsx'
import PriceMetrics from './components/PriceMetrics.jsx'
import IndicatorVotes from './components/IndicatorVotes.jsx'
import PriceChart from './components/PriceChart.jsx'
import RSIChart from './components/RSIChart.jsx'
import SignalHistory from './components/SignalHistory.jsx'
import GlobalMarket from './components/GlobalMarket.jsx'
import Portfolio from './components/Portfolio.jsx'
import TradeHistory from './components/TradeHistory.jsx'
import TradingAssistant from './components/TradingAssistant.jsx'
import ErrorBoundary from './components/ErrorBoundary.jsx'
import DashboardSkeleton from './components/DashboardSkeleton.jsx'
import Icon from './components/ui/Icon.jsx'
import { Panel, SectionHead, EmptyState } from './components/ui/Panel.jsx'
import useActiveSection from './hooks/useActiveSection.js'
import usePolling from './hooks/usePolling.js'
import {
  fetchTicker, fetchChart, fetchSignal, fetchMarket,
  fetchHistory, fetchTrades, fetchPortfolio,
} from './services/api.js'

export const SECTIONS = [
  { id: 'resumen', label: 'Resumen' },
  { id: 'graficas', label: 'Gráficas' },
  { id: 'asistente', label: 'Asistente' },
  { id: 'portafolio', label: 'Portafolio' },
  { id: 'mercado', label: 'Mercado' },
]
const SECTION_IDS = SECTIONS.map((s) => s.id)

function App() {
  const [ticker,     setTicker]     = useState(null)
  const [signal,     setSignal]     = useState(null)
  const [chart,      setChart]      = useState(null)
  const [market,     setMarket]     = useState(null)
  const [history,    setHistory]    = useState([])
  const [trades,     setTrades]     = useState([])
  const [portfolio,  setPortfolio]  = useState(null)
  const [loading,    setLoading]    = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error,      setError]      = useState(null)
  const [lastUpdate, setLastUpdate] = useState(null)
  const activeSection = useActiveSection(SECTION_IDS, !loading)

  const loadCore = useCallback(async () => {
    try {
      const [tickerData, signalData, historyData, tradesData, portfolioData] = await Promise.all([
        fetchTicker(),
        fetchSignal(),
        fetchHistory(),
        fetchTrades(),
        fetchPortfolio(),
      ])
      setTicker(tickerData)
      setSignal(signalData)
      setHistory(historyData)
      setTrades(tradesData)
      setPortfolio(portfolioData)
      setLastUpdate(new Date())
      setError(null)
    } catch (err) {
      console.error('Error loading core data:', err)
      setError(err.message)
    }
  }, [])

  const loadChart = useCallback(async () => {
    try {
      const data = await fetchChart(120)
      setChart(data)
    } catch (err) {
      console.error('Error loading chart:', err)
    }
  }, [])

  const loadMarket = useCallback(async () => {
    try {
      const data = await fetchMarket()
      setMarket(data)
    } catch (err) {
      console.error('Error loading market data:', err)
    }
  }, [])

  const loadAll = useCallback(async () => {
    await Promise.all([loadCore(), loadChart(), loadMarket()])
  }, [loadCore, loadChart, loadMarket])

  const refresh = useCallback(async () => {
    setRefreshing(true)
    await loadAll()
    setRefreshing(false)
  }, [loadAll])

  useEffect(() => {
    loadAll().finally(() => setLoading(false))
  }, [loadAll])

  // Solo con la pestaña visible: ticker, señal y portafolio cada 30 s,
  // velas cada 5 min y mercado global cada 2 min
  usePolling(loadCore, 30000)
  usePolling(loadChart, 300000)
  usePolling(loadMarket, 120000)

  if (loading) return <DashboardSkeleton />

  const hasCandles = chart?.candles?.length > 0

  return (
    <div className="app">
      <a className="skip-link" href="#contenido">Saltar al contenido</a>

      <Header
        sections={SECTIONS}
        activeSection={activeSection}
        lastUpdate={lastUpdate}
        onRefresh={refresh}
        refreshing={refreshing}
        offline={Boolean(error)}
      />

      <main className="main" id="contenido">
        {error && (
          <div className="alert" role="alert" data-testid="connection-alert">
            <Icon name="alert" />
            <div className="alert-text">
              <strong>No hay conexión con el servidor.</strong>
              <span>{error}</span>
            </div>
            <button type="button" className="btn btn-quiet" onClick={refresh} disabled={refreshing}>
              Reintentar
            </button>
          </div>
        )}

        <section className="section" id="resumen" aria-labelledby="resumen-title">
          <h2 className="visually-hidden" id="resumen-title">Resumen</h2>
          {!ticker && !signal && (
            <Panel>
              <EmptyState icon={<Icon name="pulse" size={22} />} title="Sin precio ni señal">
                El backend no respondió. Revisa que esté corriendo en el puerto 8080 y presiona Reintentar.
              </EmptyState>
            </Panel>
          )}
          <div className="bento">
            <ErrorBoundary className="span-7">
              <PriceMetrics ticker={ticker} indicators={signal?.indicators} />
            </ErrorBoundary>
            <ErrorBoundary className="span-5">
              <SignalCard signal={signal} />
            </ErrorBoundary>
            <ErrorBoundary className="span-12">
              {signal?.votes && (
                <IndicatorVotes
                  votes={signal.votes}
                  scoreBuy={signal.scoreBuy}
                  scoreSell={signal.scoreSell}
                />
              )}
            </ErrorBoundary>
          </div>
        </section>

        <section className="section" id="graficas" aria-labelledby="graficas-title">
          <SectionHead
            index="01"
            id="graficas-title"
            title="Gráficas"
            description="Velas de 1 hora de los últimos 5 días con bandas de Bollinger, y el RSI de 14 periodos."
          />
          <div className="bento">
            <div className="span-8 stack">
              <ErrorBoundary><PriceChart chart={hasCandles ? chart : null} /></ErrorBoundary>
              <ErrorBoundary>{hasCandles && <RSIChart rsi={chart.rsi} />}</ErrorBoundary>
            </div>
            <ErrorBoundary className="span-4">
              <SignalHistory history={history} />
            </ErrorBoundary>
          </div>
        </section>

        <section className="section" id="asistente" aria-labelledby="asistente-title">
          <SectionHead
            index="02"
            id="asistente-title"
            title="Asistente de análisis"
            description="Elige cualquier criptomoneda y un monto en pesos. El bot aplica las mismas estrategias y proyecta tres escenarios."
          />
          <ErrorBoundary><TradingAssistant /></ErrorBoundary>
        </section>

        <section className="section" id="portafolio" aria-labelledby="portafolio-title">
          <SectionHead
            index="03"
            id="portafolio-title"
            title="Portafolio simulado"
            description="El bot invierte el 10% del capital por operación, con Stop Loss y Take Profit calculados con el ATR."
          />
          <div className="stack">
            <ErrorBoundary><Portfolio portfolio={portfolio} /></ErrorBoundary>
            <ErrorBoundary><TradeHistory trades={trades} /></ErrorBoundary>
          </div>
        </section>

        <section className="section" id="mercado" aria-labelledby="mercado-title">
          <SectionHead
            index="04"
            id="mercado-title"
            title="Mercado global"
            description="Capitalización, dominancia y las 20 criptomonedas más grandes según CoinMarketCap."
          />
          <ErrorBoundary><GlobalMarket market={market} /></ErrorBoundary>
        </section>
      </main>

      <footer className="footer">
        <p>Proyecto educativo. Las señales son simulaciones con indicadores técnicos y no son asesoría financiera.</p>
        <p className="footer-meta">Datos de Bitso, Binance y CoinMarketCap</p>
      </footer>
    </div>
  )
}

export default App
