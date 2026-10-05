import { useEffect, useRef } from 'react'

/**
 * Llama a `callback` cada `intervalMs` mientras la pestaña está visible.
 * Con la pestaña oculta no consulta; al volver, actualiza en ese momento.
 */
export default function usePolling(callback, intervalMs) {
  const saved = useRef(callback)

  useEffect(() => {
    saved.current = callback
  }, [callback])

  useEffect(() => {
    let id = null
    const start = () => {
      if (id === null) id = setInterval(() => saved.current(), intervalMs)
    }
    const stop = () => {
      clearInterval(id)
      id = null
    }
    const onVisibility = () => {
      if (document.hidden) {
        stop()
      } else {
        saved.current()
        start()
      }
    }

    if (!document.hidden) start()
    document.addEventListener('visibilitychange', onVisibility)
    return () => {
      stop()
      document.removeEventListener('visibilitychange', onVisibility)
    }
  }, [intervalMs])
}
