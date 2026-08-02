import { apiFetch } from './client'

/** Agent research: returns a map of ticker -> shares. Slow (local LLM). */
export async function runAgentResearch() {
  const { data } = await apiFetch('/trade/stock/test/')
  return data && typeof data === 'object' ? data : {}
}

/** Research + priced execution with a $1000 allowance. Slow (local LLM). */
export async function executeAgentTrades() {
  const { data } = await apiFetch('/trade/stock/execute/')
  return data && typeof data === 'object' ? data : {}
}
