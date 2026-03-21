import './Header.css'

function Header({ lastUpdate, onRefresh }) {
  const formatTime = (date) => {
    if (!date) return 'Nunca'
    return date.toLocaleTimeString('es-MX', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    })
  }

  return (
    <header className="header">
      <div className="header-content">
        <div className="header-left">
          <h1 className="header-title">
            <span className="bitcoin-icon">₿</span>
            Mi Bot de Bitcoin
          </h1>
          <span className="header-subtitle">Dashboard de Trading — BTC/MXN</span>
        </div>

        <div className="header-right">
          <span className="last-update">
            Actualizado: {formatTime(lastUpdate)}
          </span>
          <button className="refresh-btn" onClick={onRefresh} title="Actualizar datos">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="23 4 23 10 17 10"></polyline>
              <polyline points="1 20 1 14 7 14"></polyline>
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
            </svg>
            Actualizar
          </button>
        </div>
      </div>
    </header>
  )
}

export default Header
