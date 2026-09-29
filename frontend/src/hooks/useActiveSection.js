import { useEffect, useState } from 'react'

/** Devuelve el id de la sección que ocupa el centro de la pantalla. */
/** `ready` indica que las secciones ya están en el DOM. */
export default function useActiveSection(ids, ready = true) {
  const [active, setActive] = useState(ids[0])

  useEffect(() => {
    if (!ready || typeof IntersectionObserver === 'undefined') return
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) setActive(entry.target.id)
        })
      },
      { rootMargin: '-40% 0px -55% 0px' },
    )
    ids.forEach((id) => {
      const el = document.getElementById(id)
      if (el) observer.observe(el)
    })
    return () => observer.disconnect()
  }, [ids, ready])

  return active
}
