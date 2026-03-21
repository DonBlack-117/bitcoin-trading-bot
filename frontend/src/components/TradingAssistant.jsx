import { useState, useEffect, useRef } from 'react'
import { fetchCryptoList, fetchAnalysis } from '../services/api.js'
import './TradingAssistant.css'

function ScenarioCard({ label, pct, finalMxn, initial, type }) {
  const isPositive = finalMxn >= initial
  return (
    <div className={`scenario-card scenario-${type}`}>
      <div className="scenario-label">{label}</div>
      <div className={`scenario-pct ${isPositive ? 'pos' : 'neg'}`}>
        {pct >= 0 ? '+' : ''}{pct.toFixed(1)}%
      </div>
      <div className="scenario-amount">${finalMxn.toLocaleString('es-MX', { maximumFractionDigits: 2 })} MXN</div>
      <div className="scenario-diff">
        {isPositive ? '+' : ''}${(finalMxn - initial).toLocaleString('es-MX', { maximumFractionDigits: 2 })}
      </div>
    </div>
  )
}

export default function TradingAssistant() {
  const [cryptoList,  setCryptoList]  = useState([])
  const [search,      setSearch]      = useState('')
  const [selected,    setSelected]    = useState(null)
  const [amount,      setAmount]      = useState('')
  const [showDropdown, setShowDropdown] = useState(false)
  const [loading,     setLoading]     = useState(false)
  const [result,      setResult]      = useState(null)
  const [error,       setError]       = useState(null)
  const [listLoading, setListLoading] = useState(true)
  const dropdownRef = useRef(null)

  // Load crypto list on mount
  useEffect(() => {
    fetchCryptoList()
      .then(data => { setCryptoList(data); setListLoading(false) })
      .catch(() => setListLoading(false))
  }, [])

  // Close dropdown on outside click
  useEffect(() => {
    function handleClick(e) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setShowDropdown(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  const filtered = cryptoList.filter(c =>
    c.symbol.toLowerCase().includes(search.toLowerCase()) ||
    c.name.toLowerCase().includes(search.toLowerCase())
  ).slice(0, 8)

  function selectCrypto(c) {
    setSelected(c)
    setSearch(c.symbol + ' — ' + c.name)
    setShowDropdown(false)
    setResult(null)
    setError(null)
  }

  async function handleAnalyze() {
    if (!selected)          return setError('Selecciona una criptomoneda')
    if (!amount || +amount <= 0) return setError('Ingresa un monto mayor a 0')
    setLoading(true)
    setResult(null)
    setError(null)
    try {
      const data = await fetchAnalysis(selected.symbol, parseFloat(amount))
      setResult(data)
    } catch (e) {
      setError(e.message || 'Error al analizar. Intenta de nuevo.')
    } finally {
      setLoading(false)
    }
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter') handleAnalyze()
  }

  const signalClass = result
    ? result.signal === 'COMPRAR' ? 'buy'
    : result.signal === 'VENDER'  ? 'sell'
    : 'hold'
    : ''

  return (
    <section className="ta-section">
      <div className="ta-header">
        <h2 className="ta-title">🤖 Asistente de Trading</h2>
        <p className="ta-subtitle">
          Selecciona una criptomoneda, ingresa cuánto quieres invertir y presiona Enter.
        </p>
      </div>

      {/* ── Input panel ── */}
      <div className="ta-inputs">

        {/* Crypto selector */}
        <div className="ta-field" ref={dropdownRef}>
          <label className="ta-label" htmlFor="ta-crypto-input">Criptomoneda</label>
          <input
            id="ta-crypto-input"
            className="ta-input"
            placeholder={listLoading ? 'Cargando lista...' : 'Buscar: BTC, ETH, SOL...'}
            value={search}
            disabled={listLoading}
            onChange={e => { setSearch(e.target.value); setShowDropdown(true); setSelected(null) }}
            onFocus={() => setShowDropdown(true)}
          />
          {showDropdown && filtered.length > 0 && (
            <ul className="ta-dropdown">
              {filtered.map(c => (
                <li key={c.symbol} className="ta-dropdown-item" onMouseDown={() => selectCrypto(c)}>
                  <span className="ta-dd-rank">#{c.rank}</span>
                  <span className="ta-dd-symbol">{c.symbol}</span>
                  <span className="ta-dd-name">{c.name}</span>
                  <span className={`ta-dd-pct ${c.pct24h >= 0 ? 'pos' : 'neg'}`}>
                    {c.pct24h != null ? (c.pct24h >= 0 ? '+' : '') + c.pct24h.toFixed(1) + '%' : '—'}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* Amount input */}
        <div className="ta-field">
          <label className="ta-label" htmlFor="ta-amount-input">Monto en MXN</label>
          <div className="ta-amount-wrapper">
            <span className="ta-currency">$</span>
            <input
              id="ta-amount-input"
              className="ta-input ta-input-amount"
              type="number"
              min="1"
              placeholder="1000"
              value={amount}
              onChange={e => setAmount(e.target.value)}
              onKeyDown={handleKeyDown}
            />
            <span className="ta-currency-label">MXN</span>
          </div>
        </div>

        {/* Analyze button */}
        <div className="ta-field ta-field-btn">
          <button
            className="ta-btn"
            onClick={handleAnalyze}
            disabled={loading}
          >
            {loading ? <span className="ta-spinner" role="status" aria-label="Analizando..." /> : '⚡ Analizar'}
          </button>
        </div>
      </div>

      {error && <p className="ta-error">{error}</p>}

      {/* ── Results ── */}
      {result && (
        <div className="ta-result">

          {/* Signal banner */}
          <div className={`ta-signal-banner ta-signal-${signalClass}`}>
            <span className="ta-signal-emoji">{result.emoji}</span>
            <div className="ta-signal-info">
              <span className="ta-signal-label">{result.signal}</span>
              <span className="ta-signal-name">{result.name} ({result.symbol})</span>
            </div>
            <div className="ta-confidence">
              <span className="ta-conf-num">{result.confidence}%</span>
              <span className="ta-conf-label">confianza</span>
            </div>
          </div>

          {/* Price + scenario */}
          <div className="ta-result-grid">

            {/* Left: market data + reasons */}
            <div className="ta-left">
              <div className="ta-price-row">
                <div className="ta-price-block">
                  <span className="ta-price-label">Precio actual</span>
                  <span className="ta-price-val">
                    ${result.priceMxn.toLocaleString('es-MX', { maximumFractionDigits: 2 })}
                    <span className="ta-price-cur"> MXN</span>
                  </span>
                </div>
                <div className="ta-changes">
                  <Change label="1h"  val={result.pct1h} />
                  <Change label="24h" val={result.pct24h} />
                  <Change label="7d"  val={result.pct7d} />
                </div>
              </div>

              <div className="ta-units">
                Con <strong>${parseFloat(result.amountMxn).toLocaleString('es-MX')} MXN</strong> puedes comprar{' '}
                <strong>{result.unitsToBuy < 0.01
                  ? result.unitsToBuy.toFixed(6)
                  : result.unitsToBuy.toFixed(4)
                } {result.symbol}</strong>
              </div>

              <div className="ta-reasons">
                <h4 className="ta-reasons-title">¿Por qué esta recomendación?</h4>
                <ul className="ta-reasons-list">
                  {result.reasons.map((r, i) => (
                    <li key={i} className="ta-reason-item">{r}</li>
                  ))}
                </ul>
              </div>
            </div>

            {/* Right: scenario cards */}
            <div className="ta-right">
              <h4 className="ta-scenarios-title">Escenarios proyectados</h4>
              <p className="ta-scenarios-note">
                Si inviertes ${parseFloat(result.amountMxn).toLocaleString('es-MX')} MXN ahora:
              </p>
              <div className="ta-scenarios">
                <ScenarioCard
                  label="Optimista"
                  pct={result.optimisticPct}
                  finalMxn={result.optimisticMxn}
                  initial={result.amountMxn}
                  type="optimistic"
                />
                <ScenarioCard
                  label="Esperado"
                  pct={result.expectedPct}
                  finalMxn={result.expectedMxn}
                  initial={result.amountMxn}
                  type="expected"
                />
                <ScenarioCard
                  label="Riesgo"
                  pct={result.riskPct}
                  finalMxn={result.riskMxn}
                  initial={result.amountMxn}
                  type="risk"
                />
              </div>
              <p className="ta-disclaimer">
                ⚠️ Proyecciones basadas en volatilidad reciente. No garantizan resultados futuros.
              </p>
            </div>
          </div>
        </div>
      )}
    </section>
  )
}

function Change({ label, val }) {
  if (val == null) return null
  const pos = val >= 0
  return (
    <div className={`ta-change ${pos ? 'pos' : 'neg'}`}>
      <span className="ta-change-label">{label}</span>
      <span className="ta-change-val">{pos ? '+' : ''}{val.toFixed(2)}%</span>
    </div>
  )
}
