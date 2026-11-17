import { useEffect, useRef, useState } from 'react'

/**
 * Marca el elemento como visible cuando entra en pantalla. Devuelve [ref, visible] y el que llama
 * compone la clase `is-visible`: así React no borra la clase al reescribir className (p. ej. al
 * cambiar el tono del panel), que antes dejaba el panel en opacity:0 para siempre.
 */
export default function useReveal() {
  const ref = useRef(null)
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    const node = ref.current
    if (!node) return
    if (typeof IntersectionObserver === 'undefined') {
      setVisible(true)
      return
    }
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setVisible(true)
          observer.disconnect()
        }
      },
      { rootMargin: '0px 0px -8% 0px', threshold: 0.05 },
    )
    observer.observe(node)
    return () => observer.disconnect()
  }, [])

  return [ref, visible]
}
