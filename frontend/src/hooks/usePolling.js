import { useEffect, useLayoutEffect, useRef } from 'react'

/**
 * Llama a `callback` cada `intervalMs` mientras la pestaña está visible.
 * Con la pestaña oculta no consulta; al volver, actualiza en ese momento.
 * No deja que dos llamadas se solapen ni que una promesa rechazada quede sin manejar.
 */
export default function usePolling(callback, intervalMs) {
  const saved = useRef(callback)
  const inFlight = useRef(false)

  // useLayoutEffect: el callback queda actualizado antes de que corra cualquier tick.
  useLayoutEffect(() => {
    saved.current = callback
  }, [callback])

  useEffect(() => {
    let id = null
    const run = () => {
      if (inFlight.current) return
      inFlight.current = true
      Promise.resolve(saved.current())
        .catch((err) => console.error('Error en el polling:', err))
        .finally(() => {
          inFlight.current = false
        })
    }
    const start = () => {
      if (id === null) id = setInterval(run, intervalMs)
    }
    const stop = () => {
      clearInterval(id)
      id = null
    }
    const onVisibility = () => {
      if (document.hidden) {
        stop()
      } else {
        run()
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
