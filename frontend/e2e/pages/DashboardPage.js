/** Page Object del dashboard. */
export class DashboardPage {
  constructor(page) {
    this.page = page
    this.signalCard = page.getByTestId('signal-card')
    this.signalWord = page.getByTestId('signal-word')
    this.history = page.getByTestId('signal-history')
    this.connectionAlert = page.getByTestId('connection-alert')
    this.retryButton = page.getByRole('button', { name: 'Reintentar' })
    this.assistantResult = page.getByTestId('assistant-result')
    this.assistantSignal = page.getByTestId('assistant-signal')
  }

  async goto() {
    const firstLoad = this.page.waitForResponse((r) => r.url().includes('/api/portfolio'))
    await this.page.goto('/')
    await firstLoad
  }

  async analyze(symbol, amount) {
    const assistant = this.page.locator('#asistente')
    await assistant.getByRole('combobox').fill(symbol)
    await assistant.getByRole('option', { name: new RegExp(symbol) }).first().click()
    await assistant.getByLabel(/monto/i).fill(String(amount))
    await assistant.getByRole('button', { name: /analizar/i }).click()
  }
}
