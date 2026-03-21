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
import {
  fetchTicker, fetchOhlcv, fetchSignal, fetchMarket,
  fetchHistory, fetchTrades, fetchPortfolio,
} from './services/api.js'

function App() {
  const [ticker,    setTicker]    = useState(null)
  const [signal,    setSignal]    = useState(null)
  const [ohlcv,     setOhlcv]     = useState(null)
  const [market,    setMarket]    = useState(null)
  const [history,   setHistory]   = useState([])
  const [trades,    setTrades]    = useState([])
  const [portfolio, setPortfolio] = useState(null)
  const [loading,   setLoading]   = useState(true)
  const [error,     setError]     = useState(null)
  const [lastUpdate, setLastUpdate] = useState(null)

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

  const loadOhlcv = useCallback(async () => {
    try {
      const data = await fetchOhlcv(120)
      setOhlcv(data)
    } catch (err) {
      console.error('Error loading OHLCV:', err)
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
    setLoading(true)
    await Promise.all([loadCore(), loadOhlcv(), loadMarket()])
    setLoading(false)
  }, [loadCore, loadOhlcv, loadMarket])

  useEffect(() => { loadAll() }, [loadAll])

  // Poll every 30s: core data + trades + portfolio
  useEffect(() => {
    const id = setInterval(loadCore, 30000)
    return () => clearInterval(id)
  }, [loadCore])

  // Poll every 300s: ohlcv
  useEffect(() => {
    const id = setInterval(loadOhlcv, 300000)
    return () => clearInterval(id)
  }, [loadOhlcv])

  // Poll every 120s: market
  useEffect(() => {
    const id = setInterval(loadMarket, 120000)
    return () => clearInterval(id)
  }, [loadMarket])

  if (loading) {
    return (
      <div className="loading-container">
        <div className="loading-spinner"></div>
        <p className="loading-text">Cargando datos del mercado...</p>
      </div>
    )
  }

  return (
    <div className="app">
      <Header lastUpdate={lastUpdate} onRefresh={loadAll} />

      <main className="main-content">
        {error && (
          <div className="error-banner">
            Error al conectar con el servidor: {error}
          </div>
        )}

        <div className="top-row">
          <ErrorBoundary>{signal && <SignalCard signal={signal} />}</ErrorBoundary>
          <ErrorBoundary>{ticker && <PriceMetrics ticker={ticker} />}</ErrorBoundary>
        </div>

        <ErrorBoundary>
          {signal && signal.votes && (
            <IndicatorVotes
              votes={signal.votes}
              scoreBuy={signal.scoreBuy}
              scoreSell={signal.scoreSell}
            />
          )}
        </ErrorBoundary>

        <ErrorBoundary>
          {ohlcv && ohlcv.length > 0 && <PriceChart ohlcv={ohlcv} />}
        </ErrorBoundary>

        <ErrorBoundary><TradingAssistant /></ErrorBoundary>

        <div className="bottom-row">
          <ErrorBoundary>{ohlcv && ohlcv.length > 0 && <RSIChart ohlcv={ohlcv} />}</ErrorBoundary>
          <ErrorBoundary><SignalHistory history={history} /></ErrorBoundary>
        </div>

        <ErrorBoundary><Portfolio portfolio={portfolio} /></ErrorBoundary>

        <ErrorBoundary><TradeHistory trades={trades} /></ErrorBoundary>

        <ErrorBoundary>{market && <GlobalMarket market={market} />}</ErrorBoundary>
      </main>

      <footer className="app-footer">
        <p>Este dashboard es solo educativo. No es asesoría financiera.</p>
      </footer>
    </div>
  )
}

export default App
