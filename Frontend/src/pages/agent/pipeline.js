/** Ordered stages the supervisor is expected to spawn (docs/agent-trader-v2.md). */
export const PIPELINE_STAGES = [
  { id: 'news_scout', label: 'News Scout', call: 'Finnhub news', kind: 'api' },
  { id: 'bull', label: 'Bull Agent', call: 'Groq chat', kind: 'llm' },
  { id: 'bear', label: 'Bear Agent', call: 'Groq chat', kind: 'llm' },
  { id: 'stock_judge', label: 'Stock Judge', call: 'Groq chat', kind: 'llm' },
  { id: 'quote_snapshot', label: 'Quote Snapshot', call: 'Finnhub quote', kind: 'api' },
  { id: 'allocator_aggressive', label: 'Aggressive Allocator', call: 'rules', kind: 'rules' },
  { id: 'allocator_balanced', label: 'Balanced Allocator', call: 'rules', kind: 'rules' },
  { id: 'allocator_defensive', label: 'Defensive Allocator', call: 'rules', kind: 'rules' },
  { id: 'cash_guard', label: 'Cash Guard', call: 'rules', kind: 'rules' },
  { id: 'capital_judge', label: 'Capital Judge', call: 'Groq chat', kind: 'llm' },
  { id: 'position_sizer', label: 'Position Sizer', call: 'rules', kind: 'rules' },
  { id: 'risk_gate', label: 'Risk / Book Gate', call: 'rules', kind: 'rules' },
  { id: 'executor', label: 'Trade Executor', call: 'simulated fill', kind: 'exec' },
]

const META_BY_ID = Object.fromEntries(PIPELINE_STAGES.map((s) => [s.id, s]))

export function agentMeta(agentId) {
  if (META_BY_ID[agentId]) return META_BY_ID[agentId]
  if (agentId === 'supervisor') {
    return { id: 'supervisor', label: 'Run Supervisor', call: 'orchestration', kind: 'rules' }
  }
  return {
    id: agentId,
    label: String(agentId || 'agent')
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' '),
    call: 'step',
    kind: 'rules',
  }
}

/**
 * Merge expected pipeline with live/completed agentTrace steps.
 * @param {Array<{agent?: string, status?: string, summary?: string}>|null|undefined} steps
 * @param {{ running?: boolean, mode?: 'research'|'execute'|null }} opts
 */
export function buildPipelineView(steps, { running = false, mode = 'execute' } = {}) {
  const expected = PIPELINE_STAGES.filter((stage) => {
    if (stage.id === 'executor') return mode !== 'research'
    return true
  })

  const byAgent = new Map()
  for (const step of Array.isArray(steps) ? steps : []) {
    if (step?.agent) byAgent.set(step.agent, step)
  }

  let sawActive = false
  return expected.map((stage) => {
    const match = byAgent.get(stage.id)
    if (match) {
      return {
        ...stage,
        phase: 'done',
        status: match.status ?? 'ok',
        summary: match.summary ?? '',
        step: match,
      }
    }
    if (running && !sawActive) {
      sawActive = true
      return { ...stage, phase: 'active', status: 'running', summary: `Calling ${stage.call}…`, step: null }
    }
    if (running) {
      return { ...stage, phase: 'queued', status: 'queued', summary: 'Waiting to spawn', step: null }
    }
    return { ...stage, phase: 'skipped', status: 'skipped', summary: 'Did not run', step: null }
  })
}

/** Pretty, bounded detail rendering input for the expandable step body. */
export function detailEntries(detail) {
  if (detail == null) return []
  if (typeof detail !== 'object' || Array.isArray(detail)) {
    return [{ key: 'value', value: detail }]
  }
  return Object.entries(detail).map(([key, value]) => ({ key, value }))
}

export function isEmptyDetail(detail) {
  if (detail == null) return true
  if (Array.isArray(detail)) return detail.length === 0
  if (typeof detail === 'object') return Object.keys(detail).length === 0
  return false
}
