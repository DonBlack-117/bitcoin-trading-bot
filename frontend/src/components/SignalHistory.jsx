import './SignalHistory.css'

function SignalBadge({ senal }) {
  const map = {
    COMPRAR: { cls: 'badge-buy', label: 'COMPRAR' },
    VENDER: { cls: 'badge-sell', label: 'VENDER' },
    MANTENER: { cls: 'badge-hold', label: 'MANTENER' },
  }
  const { cls, label } = map[senal] || { cls: 'badge-hold', label: senal }
  return <span className={`signal-badge ${cls}`}>{label}</span>
}

function SignalHistory({ history }) {
  const formatDate = (ts) => {
    if (!ts) return 'N/A'
    const d = new Date(ts)
    return d.toLocaleString('es-MX', {
      day: '2-digit',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  const formatMxn = (value) =>
    new Intl.NumberFormat('es-MX', {
      style: 'currency',
      currency: 'MXN',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value)

  return (
    <div className="signal-history card">
      <h2 className="history-title">Historial de Señales</h2>

      {!history || history.length === 0 ? (
        <div className="history-empty">
          <span className="empty-icon">📭</span>
          <span>No hay señales registradas aún</span>
        </div>
      ) : (
        <ul className="history-list">
          {history.map((item, index) => (
            <li key={item.id || index} className="history-item">
              <div className="history-left">
                <SignalBadge senal={item.senal} />
                <span className="history-date">{formatDate(item.timestamp)}</span>
              </div>
              <div className="history-right">
                <span className="history-price">{formatMxn(item.precio)}</span>
                <span className="history-confidence">{item.confianza}%</span>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default SignalHistory
