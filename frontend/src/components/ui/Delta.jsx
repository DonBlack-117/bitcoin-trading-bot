import Icon from './Icon.jsx'
import { fmtPct, trendOf } from '../../utils/format.js'

/** Cambio porcentual con flecha y signo, para no depender solo del color. */
export default function Delta({ value, digits = 2, suffix, className = '' }) {
  const trend = trendOf(value)
  const icon = trend === 'up' ? 'arrowUp' : trend === 'down' ? 'arrowDown' : 'minus'
  return (
    <span className={`delta delta-${trend} ${className}`}>
      <Icon name={icon} size={13} />
      {fmtPct(value, digits)}
      {suffix && <span className="delta-suffix">{suffix}</span>}
    </span>
  )
}
