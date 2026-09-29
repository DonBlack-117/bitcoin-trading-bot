function Block({ className = '' }) {
  return <div className={`skeleton ${className}`} />
}

/** Esqueleto con la misma forma que el resumen mientras llega la primera respuesta. */
export default function DashboardSkeleton() {
  return (
    <div className="app" aria-busy="true" aria-live="polite">
      <div className="topbar">
        <div className="topbar-inner">
          <Block className="sk-brand" />
          <Block className="sk-nav" />
          <Block className="sk-status" />
        </div>
      </div>
      <main className="main">
        <p className="visually-hidden">Cargando datos del mercado</p>
        <div className="bento">
          <div className="span-7 panel"><div className="panel-core sk-core">
            <Block className="sk-line sk-w30" />
            <Block className="sk-hero" />
            <Block className="sk-line sk-w60" />
            <div className="sk-row"><Block className="sk-box" /><Block className="sk-box" /></div>
          </div></div>
          <div className="span-5 panel"><div className="panel-core sk-core">
            <Block className="sk-line sk-w30" />
            <Block className="sk-hero sk-w60" />
            <Block className="sk-line" />
            <Block className="sk-line sk-w80" />
            <Block className="sk-bar" />
          </div></div>
          <div className="span-12 panel"><div className="panel-core sk-core">
            <Block className="sk-line sk-w30" />
            <Block className="sk-line" /><Block className="sk-line" /><Block className="sk-line" />
          </div></div>
        </div>
      </main>
    </div>
  )
}
