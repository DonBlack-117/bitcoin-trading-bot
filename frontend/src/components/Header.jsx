import Icon from './ui/Icon.jsx'
import { fmtTime } from '../utils/format.js'
import './Header.css'

function Header({ sections, activeSection, lastUpdate, onRefresh, refreshing, offline }) {
  return (
    <header className="topbar">
      <div className="topbar-inner">
        <a className="brand" href="#resumen">
          <span className="brand-mark" aria-hidden="true">₿</span>
          <span className="brand-text">
            <strong>Bot de Bitcoin</strong>
            <span>BTC/MXN · Bitso</span>
          </span>
        </a>

        <nav className="nav-island" aria-label="Secciones">
          {sections.map(({ id, label }) => (
            <a
              key={id}
              href={`#${id}`}
              className="nav-link"
              aria-current={activeSection === id ? 'true' : undefined}
            >
              {label}
            </a>
          ))}
        </nav>

        <div className="topbar-status">
          <span className={`live ${offline ? 'is-offline' : ''}`}>
            <span className="live-dot" aria-hidden="true" />
            <span className="live-label">{offline ? 'Sin conexión' : 'En vivo'}</span>
            <time className="live-time">{fmtTime(lastUpdate)}</time>
          </span>
          <button
            type="button"
            className="btn-icon"
            onClick={onRefresh}
            disabled={refreshing}
            aria-label="Actualizar datos"
            title="Actualizar datos"
          >
            <Icon name="refresh" className={refreshing ? 'is-spinning' : ''} />
          </button>
        </div>
      </div>
    </header>
  )
}

export default Header
