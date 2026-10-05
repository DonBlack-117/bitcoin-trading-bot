import { useState, useEffect, useRef, useId } from 'react'
import { fetchCryptoList, fetchAnalysis } from '../services/api.js'
import { Panel } from './ui/Panel.jsx'
import Icon from './ui/Icon.jsx'
import Delta from './ui/Delta.jsx'
import { fmtMxn, fmtPct, toneOfStrength } from '../utils/format.js'
import './TradingAssistant.css'

const SIGNAL_ICON = { buy: 'arrowUp', sell: 'arrowDown', hold: 'minus' }

function Scenario({ label, pct, finalMxn, initial, kind }) {
  const diff = finalMxn - initial
  return (
    <div className={`scenario scenario-${kind}`}>
      <div className="scenario-head">
        <span className="scenario-label">{label}</span>
        <span className="scenario-pct">{fmtPct(pct, 1)}</span>
      </div>
      <span className="scenario-amount">{fmtMxn(finalMxn)}</span>
      <span className={`scenario-diff pnl pnl-${diff > 0 ? 'up' : diff < 0 ? 'down' : 'flat'}`}>
        {diff > 0 ? '+' : ''}{fmtMxn(diff)}
      </span>
    </div>
  )
}

export default function TradingAssistant() {
  const [cryptoList,   setCryptoList]   = useState([])
  const [search,       setSearch]       = useState('')
  const [selected,     setSelected]     = useState(null)
  const [amount,       setAmount]       = useState('')
  const [showDropdown, setShowDropdown] = useState(false)
  const [highlight,    setHighlight]    = useState(0)
  const [loading,      setLoading]      = useState(false)
  const [result,       setResult]       = useState(null)
  const [error,        setError]        = useState(null)
  const [fieldError,   setFieldError]   = useState({})
  const [listLoading,  setListLoading]  = useState(true)
  const [listFailed,   setListFailed]   = useState(false)
  const comboRef = useRef(null)
  const amountRef = useRef(null)
  const listId = useId()

  useEffect(() => {
    fetchCryptoList()
      .then((data) => setCryptoList(data))
      .catch(() => setListFailed(true))
      .finally(() => setListLoading(false))
  }, [])

  useEffect(() => {
    function handleClick(e) {
      if (comboRef.current && !comboRef.current.contains(e.target)) setShowDropdown(false)
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  const query = search.toLowerCase()
  const filtered = cryptoList
    .filter((c) => c.symbol.toLowerCase().includes(query) || c.name.toLowerCase().includes(query))
    .slice(0, 8)

  function selectCrypto(c) {
    setSelected(c)
    setSearch(`${c.symbol} · ${c.name}`)
    setShowDropdown(false)
    setResult(null)
    setError(null)
    setFieldError((f) => ({ ...f, crypto: null }))
    amountRef.current?.focus()
  }

  function handleComboKey(e) {
    if (!showDropdown && (e.key === 'ArrowDown' || e.key === 'ArrowUp')) {
      setShowDropdown(true)
      return
    }
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setHighlight((h) => Math.min(h + 1, filtered.length - 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setHighlight((h) => Math.max(h - 1, 0))
    } else if (e.key === 'Enter' && showDropdown && filtered[highlight]) {
      e.preventDefault()
      selectCrypto(filtered[highlight])
    } else if (e.key === 'Escape') {
      setShowDropdown(false)
    }
  }

  async function handleAnalyze(e) {
    e?.preventDefault()
    const errors = {}
    if (!selected) errors.crypto = 'Elige una criptomoneda de la lista.'
    if (!amount || +amount <= 0) errors.amount = 'Escribe un monto mayor a 0.'
    setFieldError(errors)
    if (errors.crypto || errors.amount) return

    setLoading(true)
    setResult(null)
    setError(null)
    try {
      const data = await fetchAnalysis(selected.symbol, parseFloat(amount))
      setResult(data)
    } catch (err) {
      setError(err.message || 'No se pudo analizar. Intenta de nuevo.')
    } finally {
      setLoading(false)
    }
  }

  const tone = result ? toneOfStrength(result.signal, result.strong) : 'hold'
  const activeOption = showDropdown && filtered[highlight] ? `${listId}-${filtered[highlight].symbol}` : undefined

  return (
    <div className="assistant">
      <Panel className="assistant-form-panel">
        <form className="assistant-form" onSubmit={handleAnalyze} noValidate>
          <div className={`field ${fieldError.crypto ? 'has-error' : ''}`} ref={comboRef}>
            <label className="field-label" htmlFor="ta-crypto">Criptomoneda</label>
            <div className="input-shell">
              <Icon name="search" size={16} />
              <input
                id="ta-crypto"
                className="input"
                role="combobox"
                aria-expanded={showDropdown && filtered.length > 0}
                aria-controls={listId}
                aria-autocomplete="list"
                aria-activedescendant={activeOption}
                aria-invalid={Boolean(fieldError.crypto)}
                autoComplete="off"
                placeholder={listLoading ? 'Cargando lista…' : listFailed ? 'No se pudo cargar la lista' : 'BTC, ETH, SOL…'}
                value={search}
                disabled={listLoading || listFailed}
                onChange={(e) => {
                  setSearch(e.target.value)
                  setShowDropdown(true)
                  setSelected(null)
                  setHighlight(0)
                }}
                onFocus={() => setShowDropdown(true)}
                onKeyDown={handleComboKey}
              />
            </div>
            {showDropdown && filtered.length > 0 && (
              <ul className="combo-list" id={listId} role="listbox">
                {filtered.map((c, i) => (
                  <li
                    key={c.symbol}
                    id={`${listId}-${c.symbol}`}
                    role="option"
                    aria-selected={i === highlight}
                    className="combo-option"
                    onMouseDown={() => selectCrypto(c)}
                    onMouseEnter={() => setHighlight(i)}
                  >
                    <span className="combo-rank">{c.rank}</span>
                    <span className="combo-name"><b>{c.symbol}</b> <span>{c.name}</span></span>
                    <Delta value={c.pct24h} digits={1} />
                  </li>
                ))}
              </ul>
            )}
            {fieldError.crypto && <p className="field-error">{fieldError.crypto}</p>}
          </div>

          <div className={`field ${fieldError.amount ? 'has-error' : ''}`}>
            <label className="field-label" htmlFor="ta-amount">Monto a invertir</label>
            <div className="input-shell">
              <span className="input-affix">$</span>
              <input
                id="ta-amount"
                ref={amountRef}
                className="input input-num"
                type="number"
                inputMode="decimal"
                min="1"
                placeholder="1,000"
                aria-invalid={Boolean(fieldError.amount)}
                value={amount}
                onChange={(e) => {
                  setAmount(e.target.value)
                  setFieldError((f) => ({ ...f, amount: null }))
                }}
              />
              <span className="input-affix">MXN</span>
            </div>
            {fieldError.amount && <p className="field-error">{fieldError.amount}</p>}
          </div>

          <button className="btn btn-primary" type="submit" disabled={loading}>
            <span>{loading ? 'Analizando…' : 'Analizar'}</span>
            <span className="btn-nub" aria-hidden="true">
              <Icon name={loading ? 'refresh' : 'arrowRight'} size={16} className={loading ? 'is-spinning' : ''} />
            </span>
          </button>
        </form>
        {error && <p className="form-error" role="alert"><Icon name="alert" size={16} /> {error}</p>}
      </Panel>

      {result && (
        <div className={`assistant-result tone-${tone}`} aria-live="polite" data-testid="assistant-result">
          <Panel className="result-verdict">
            <p className="eyebrow">{result.name} ({result.symbol})</p>
            <div className="verdict">
              <span className="verdict-icon" aria-hidden="true"><Icon name={SIGNAL_ICON[tone]} size={24} /></span>
              <span className="verdict-word" data-testid="assistant-signal">{result.signal}</span>
              {!result.strong && result.signal !== 'MANTENER' && (
                <span className="signal-weak" title="Inclinación débil: conviene esperar una confirmación">Débil</span>
              )}
              <span className="verdict-conf"><b>{result.confidence}%</b> confianza</span>
            </div>

            <div className="result-price">
              <div className="stat">
                <span className="stat-label">Precio actual</span>
                <span className="stat-value">{fmtMxn(result.priceMxn)}</span>
              </div>
              <div className="result-changes">
                {result.pct1h != null && <span><small>1 h</small><Delta value={result.pct1h} /></span>}
                {result.pct24h != null && <span><small>24 h</small><Delta value={result.pct24h} /></span>}
                {result.pct7d != null && <span><small>7 d</small><Delta value={result.pct7d} /></span>}
              </div>
            </div>

            <p className="result-units">
              Con <b>{fmtMxn(parseFloat(result.amountMxn))}</b> compras{' '}
              <b>{result.unitsToBuy < 0.01 ? result.unitsToBuy.toFixed(6) : result.unitsToBuy.toFixed(4)} {result.symbol}</b>.
            </p>

            <h4 className="reasons-title">Por qué</h4>
            <ul className="reasons">
              {result.reasons.map((r, i) => <li key={i}>{r}</li>)}
            </ul>
          </Panel>

          <Panel className="result-scenarios">
            <p className="eyebrow">Si inviertes {fmtMxn(parseFloat(result.amountMxn))} hoy</p>
            <h4 className="panel-title">Escenarios proyectados</h4>
            <div className="scenarios">
              <Scenario label="Optimista" kind="optimistic" pct={result.optimisticPct} finalMxn={result.optimisticMxn} initial={result.amountMxn} />
              <Scenario label="Esperado" kind="expected" pct={result.expectedPct} finalMxn={result.expectedMxn} initial={result.amountMxn} />
              <Scenario label="Riesgo" kind="risk" pct={result.riskPct} finalMxn={result.riskMxn} initial={result.amountMxn} />
            </div>
            <p className="scenarios-note">Proyecciones con la volatilidad reciente. No garantizan resultados.</p>
          </Panel>
        </div>
      )}
    </div>
  )
}
