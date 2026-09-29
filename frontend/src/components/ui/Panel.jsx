import useReveal from '../../hooks/useReveal.js'

/** Contenedor de doble bisel: carcasa exterior + núcleo interior. */
export function Panel({ as: Tag = 'article', className = '', coreClassName = '', children, ...rest }) {
  const ref = useReveal()
  return (
    <Tag ref={ref} className={`panel reveal ${className}`} {...rest}>
      <div className={`panel-core ${coreClassName}`}>{children}</div>
    </Tag>
  )
}

export function PanelHead({ eyebrow, title, id, children }) {
  return (
    <header className="panel-head">
      <div className="panel-head-text">
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h3 className="panel-title" id={id}>{title}</h3>
      </div>
      {children && <div className="panel-head-actions">{children}</div>}
    </header>
  )
}

export function SectionHead({ index, title, description, id }) {
  return (
    <header className="section-head">
      <p className="section-index"><span>{index}</span></p>
      <div>
        <h2 className="section-title" id={id}>{title}</h2>
        {description && <p className="section-desc">{description}</p>}
      </div>
    </header>
  )
}

export function EmptyState({ icon, title, children }) {
  return (
    <div className="empty-state">
      <span className="empty-icon">{icon}</span>
      <p className="empty-title">{title}</p>
      {children && <p className="empty-text">{children}</p>}
    </div>
  )
}
