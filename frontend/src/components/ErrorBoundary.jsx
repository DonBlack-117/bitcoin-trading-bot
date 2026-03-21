import { Component } from 'react'

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
    if (this.state.hasError) {
      return (
        <div style={{
          padding: '24px',
          background: 'rgba(239,68,68,0.08)',
          border: '1px solid rgba(239,68,68,0.3)',
          borderRadius: '12px',
          margin: '16px 0',
          color: '#f87171',
        }}>
          <strong>Algo salió mal en este componente.</strong>
          <pre style={{ fontSize: '12px', marginTop: '8px', color: '#9ca3af', whiteSpace: 'pre-wrap' }}>
            {this.state.error?.message}
          </pre>
        </div>
      )
    }
    return this.props.children
  }
}
