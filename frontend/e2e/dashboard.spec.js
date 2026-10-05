import { test, expect } from '@playwright/test'
import { mockApi, data, apiError } from './fixtures/api.js'
import { DashboardPage } from './pages/DashboardPage.js'

test.describe('Dashboard', () => {
  test('muestra precio, señal, historial y portafolio de la API', async ({ page }) => {
    const calls = await mockApi(page)
    const dashboard = new DashboardPage(page)
    await dashboard.goto()

    await expect(dashboard.signalWord).toHaveText('COMPRAR')
    await expect(dashboard.signalCard).toHaveClass(/tone-buy/)
    await expect(dashboard.signalCard).not.toContainText('Débil')
    await expect(dashboard.history.locator('li')).toHaveCount(2)
    await expect(dashboard.history).toContainText('Vender')
    await expect(dashboard.connectionAlert).toHaveCount(0)

    // Las gráficas usan las series del backend: ya no se pide /ohlcv
    await expect(page.locator('#graficas .apexcharts-canvas').first()).toBeVisible()
    expect(calls).toContain('/chart')
    expect(calls).not.toContain('/ohlcv')
  })

  test('una señal con la que el bot no opera sale como débil y en ámbar', async ({ page }) => {
    await mockApi(page, {
      '/signal': { body: { ...data.signal, signal: 'VENDER', confidence: 53, actionable: false } },
    })
    const dashboard = new DashboardPage(page)
    await dashboard.goto()

    await expect(dashboard.signalWord).toHaveText('VENDER')
    await expect(dashboard.signalCard).toHaveClass(/tone-hold/)
    await expect(dashboard.signalCard).toContainText('Débil')
  })

  test('mientras se calcula la primera señal no marca error de conexión', async ({ page }) => {
    await mockApi(page, {
      '/signal': { status: 503, body: apiError('SIGNAL_NOT_READY', 'La primera señal se está calculando') },
    })
    const dashboard = new DashboardPage(page)
    await dashboard.goto()

    await expect(dashboard.connectionAlert).toHaveCount(0)
    await expect(dashboard.signalCard).toHaveCount(0)
    await expect(page.locator('#portafolio')).toContainText('45,000')
  })

  test('sin backend avisa, y Reintentar carga los datos cuando vuelve', async ({ page }) => {
    let backendUp = false
    await mockApi(page, {
      '/ticker': (route) => (backendUp ? route.fulfill({ json: data.ticker }) : route.abort('connectionrefused')),
    })
    await page.goto('/')
    const dashboard = new DashboardPage(page)

    await expect(dashboard.connectionAlert).toBeVisible()
    await expect(dashboard.connectionAlert).toContainText('No hay conexión con el servidor')

    backendUp = true
    await dashboard.retryButton.click()
    await expect(dashboard.connectionAlert).toHaveCount(0)
    await expect(dashboard.signalWord).toHaveText('COMPRAR')
  })

  test('deja de consultar con la pestaña oculta y actualiza al volver', async ({ page }) => {
    await page.clock.install()
    const calls = await mockApi(page)
    const dashboard = new DashboardPage(page)
    await dashboard.goto()

    const tickerCalls = () => calls.filter((c) => c === '/ticker').length
    const setHidden = (hidden) => page.evaluate((h) => {
      Object.defineProperty(document, 'hidden', { configurable: true, get: () => h })
      document.dispatchEvent(new Event('visibilitychange'))
    }, hidden)

    // En dev, StrictMode hace la carga inicial dos veces: se cuenta desde lo que haya
    await expect.poll(tickerCalls).toBeGreaterThan(0)
    const initial = tickerCalls()

    await page.clock.runFor(30_000)
    await expect.poll(tickerCalls).toBe(initial + 1)

    await setHidden(true)
    await page.clock.runFor(120_000)
    expect(tickerCalls()).toBe(initial + 1)

    await setHidden(false)
    await expect.poll(tickerCalls).toBe(initial + 2)
  })
})

test.describe('Asistente', () => {
  test('pide los datos antes de consultar', async ({ page }) => {
    const calls = await mockApi(page)
    await new DashboardPage(page).goto()

    await page.locator('#asistente').getByRole('button', { name: /analizar/i }).click()

    await expect(page.locator('#asistente')).toContainText('Elige una criptomoneda de la lista.')
    await expect(page.locator('#asistente')).toContainText('Escribe un monto mayor a 0.')
    expect(calls).not.toContain('/analysis')
  })

  test('muestra la recomendación', async ({ page }) => {
    await mockApi(page)
    const dashboard = new DashboardPage(page)
    await dashboard.goto()

    await dashboard.analyze('ETH', 1000)

    await expect(dashboard.assistantSignal).toHaveText('COMPRAR')
    await expect(dashboard.assistantResult).toHaveClass(/tone-buy/)
    await expect(dashboard.assistantResult).toContainText('92%')
    await expect(dashboard.assistantResult).not.toContainText('Débil')
  })

  test('una inclinación débil se marca como tal', async ({ page }) => {
    await mockApi(page, { '/analysis': { body: { ...data.analysis, strong: false, confidence: 60 } } })
    const dashboard = new DashboardPage(page)
    await dashboard.goto()

    await dashboard.analyze('ETH', 1000)

    await expect(dashboard.assistantResult).toHaveClass(/tone-hold/)
    await expect(dashboard.assistantResult).toContainText('Débil')
  })

  test('muestra el mensaje de error del backend', async ({ page }) => {
    await mockApi(page, {
      '/analysis': { status: 404, body: apiError('CRYPTO_NOT_FOUND', 'La criptomoneda ETH no está en el top 100 de CoinMarketCap') },
    })
    const dashboard = new DashboardPage(page)
    await dashboard.goto()

    await dashboard.analyze('ETH', 1000)

    await expect(page.locator('#asistente [role="alert"]')).toContainText('no está en el top 100')
  })
})

test('el diseño no se sale de la pantalla', async ({ page }) => {
  await mockApi(page)
  await new DashboardPage(page).goto()

  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
  expect(overflow).toBeLessThanOrEqual(0)
})
