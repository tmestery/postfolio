import { act } from 'react'
import { createRoot } from 'react-dom/client'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AgentPage from './index'

globalThis.IS_REACT_ACT_ENVIRONMENT = true

vi.mock('../../auth/useAuth', () => ({
  useAuth: () => ({ user: { username: 'demo' }, isAuthenticated: true }),
}))

const trades = vi.hoisted(() => ({
  runAgentResearch: vi.fn(),
  executeAgentTrades: vi.fn(),
  listAgentRuns: vi.fn(),
  getAgentRun: vi.fn(),
}))

vi.mock('../../api/trades', () => trades)

const sampleResult = {
  runId: 'abc',
  status: 'completed',
  startingAllowance: 1000,
  cashReserveTarget: 150,
  totalInvested: 240,
  remainingAllowance: 760,
  candidates: [{ ticker: 'NVDA', thesis: 'AI demand', score: 8.1 }],
  allocatorProposals: {
    aggressive: { NVDA: 400 },
    balanced: { NVDA: 250, AAPL: 250 },
    defensive: { AAPL: 200 },
  },
  capitalJudgeDecision: {
    approved: { NVDA: 350 },
    winnerStyle: 'blend',
    rationale: 'Mild overweight on NVDA',
    cashHeld: 650,
  },
  plannedShares: { NVDA: 2 },
  executedTrades: { NVDA: { shares: 2, price: 120, cost: 240 } },
  agentTrace: [
    { step: 1, agent: 'news_scout', status: 'ok', summary: 'Fetched 60 headlines' },
    { step: 2, agent: 'bull', status: 'ok', summary: 'Proposed 1 candidate(s)' },
  ],
}

async function renderPage() {
  const container = document.createElement('div')
  document.body.appendChild(container)
  await act(async () => {
    createRoot(container).render(<AgentPage />)
  })
  return container
}

async function clickButton(container, label) {
  const button = [...container.querySelectorAll('button')].find((b) =>
    b.textContent.toLowerCase().includes(label),
  )
  await act(async () => {
    button.click()
  })
}

describe('AgentPage', () => {
  beforeEach(() => {
    trades.listAgentRuns.mockResolvedValue([])
    trades.runAgentResearch.mockReset()
    trades.executeAgentTrades.mockReset()
  })

  // Positive: execute path renders desk window with spawns + fills (no call trace).
  it('renders mocked execute result inside the desk window', async () => {
    trades.executeAgentTrades.mockResolvedValue(sampleResult)
    const container = await renderPage()
    await clickButton(container, 'execute')

    expect(container.textContent).toContain('News Scout')
    expect(container.textContent).toContain('Spawns')
    expect(container.textContent).not.toMatch(/call trace/i)
    expect(container.textContent).toMatch(/capital judge/i)
    expect(container.textContent).toContain('Mild overweight on NVDA')
    expect(container.textContent).toContain('$240.00')
    container.remove()
  })

  // Negative: empty trace still shows calm empty copy inside the desk.
  it('shows empty-steps message when agentTrace is missing', async () => {
    trades.runAgentResearch.mockResolvedValue({
      ...sampleResult,
      agentTrace: [],
      executedTrades: {},
      totalInvested: 0,
    })
    const container = await renderPage()
    await clickButton(container, 'research')

    expect(container.textContent).toMatch(/no agent steps recorded/i)
    container.remove()
  })

  // Negative: 503-style error body surfaces the message.
  it('shows 503-style error message', async () => {
    trades.executeAgentTrades.mockResolvedValue({
      error: 'GROQ_API_KEY is not configured on the server',
    })
    const container = await renderPage()
    await clickButton(container, 'execute')

    expect(container.textContent).toMatch(/GROQ_API_KEY is not configured/i)
    container.remove()
  })

  // Negative: partial status shows warning banner without crashing.
  it('shows partial-run warning', async () => {
    trades.executeAgentTrades.mockResolvedValue({
      ...sampleResult,
      status: 'partial',
      error: 'Run exceeded the wall-clock budget',
    })
    const container = await renderPage()
    await clickButton(container, 'execute')

    expect(container.textContent).toMatch(/partial run/i)
    expect(container.textContent).toMatch(/wall-clock budget/i)
    container.remove()
  })
})
