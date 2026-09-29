import { Component } from 'react'
import Icon from './ui/Icon.jsx'

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, info) {
    console.error('ErrorBoundary caught:', error, info)
  }

  render() {
    const { className = '', children } = this.props
    if (this.state.hasError) {
      return (
        <div className={`boundary-error ${className}`} role="alert">
          <Icon name="alert" />
          <div>
            <strong>Este panel no se pudo mostrar.</strong>
            <pre>{this.state.error?.message}</pre>
          </div>
        </div>
      )
    }
    if (!className) return children
    return <div className={className}>{children}</div>
  }
}
