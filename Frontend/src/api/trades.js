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
