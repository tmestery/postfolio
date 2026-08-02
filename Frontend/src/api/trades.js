import { apiFetch } from './client'

/** Research run: full pipeline through Risk Gate, paper book only (no fills). */
export async function runAgentResearch(username) {
  const qs = username ? `?username=${encodeURIComponent(username)}` : ''
  const { data } = await apiFetch(`/trade/stock/test/${qs}`)
  return data && typeof data === 'object' ? data : {}
}

/** Full run including simulated fills + agentTrace. */
export async function executeAgentTrades(username) {
  const qs = username ? `?username=${encodeURIComponent(username)}` : ''
  const { data } = await apiFetch(`/trade/stock/execute/${qs}`)
  return data && typeof data === 'object' ? data : {}
}

/** Recent agent run summaries. */
export async function listAgentRuns() {
  const { data } = await apiFetch('/trade/runs/')
  return Array.isArray(data) ? data : []
}

/** Full persisted RunResult for a past run (agent desk history). */
export async function getAgentRun(runId) {
  const { data } = await apiFetch(`/trade/runs/${encodeURIComponent(runId)}/`)
  return data && typeof data === 'object' ? data : {}
}

/** House paper portfolio snapshot (docs/agent-trader-v4.md). */
export async function getPortfolio() {
  const { data } = await apiFetch('/trade/portfolio/')
  return data && typeof data === 'object' ? data : {}
}

export async function refreshPortfolio() {
  try {
    const { data } = await apiFetch('/trade/portfolio/refresh/', { method: 'POST' })
    return data && typeof data === 'object' ? data : {}
  } catch (err) {
    throw err instanceof Error ? err : new Error('Refresh failed')
  }
}

export async function resetPortfolio() {
  const { data } = await apiFetch('/trade/portfolio/reset/', { method: 'POST' })
  return data && typeof data === 'object' ? data : {}
}

export async function getPortfolioHistory() {
  const { data } = await apiFetch('/trade/portfolio/history/')
  return data?.points && Array.isArray(data.points) ? data.points : []
}
