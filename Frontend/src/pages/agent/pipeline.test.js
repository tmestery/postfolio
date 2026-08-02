import { describe, expect, it } from 'vitest'
import {
  agentMeta,
  buildPipelineView,
  detailEntries,
  isEmptyDetail,
  PIPELINE_STAGES,
} from './pipeline'

describe('buildPipelineView', () => {
  it('marks completed stages from the trace and leaves later ones skipped', () => {
    const view = buildPipelineView(
      [
        { agent: 'source_planner', status: 'ok', summary: 'Planned 3' },
        { agent: 'web_scout', status: 'ok', summary: 'Fetched 2' },
      ],
      { running: false, mode: 'research' },
    )
    expect(view[0].phase).toBe('done')
    expect(view[1].phase).toBe('done')
    expect(view[2].phase).toBe('skipped')
    expect(view.some((s) => s.id === 'executor')).toBe(false)
  })

  it('keeps executor only on execute mode', () => {
    const research = buildPipelineView([], { mode: 'research' })
    const execute = buildPipelineView([], { mode: 'execute' })
    expect(research.some((s) => s.id === 'executor')).toBe(false)
    expect(execute.some((s) => s.id === 'executor')).toBe(true)
  })

  it('while running, first unfinished stage is active and the rest are queued', () => {
    const view = buildPipelineView(
      [{ agent: 'source_planner', status: 'ok', summary: 'Planned 3' }],
      { running: true, mode: 'execute' },
    )
    expect(view[0].phase).toBe('done')
    expect(view[1].phase).toBe('active')
    expect(view[2].phase).toBe('queued')
  })

  it('treats a null/non-array trace as empty without throwing', () => {
    expect(buildPipelineView(null, { running: false })).toHaveLength(PIPELINE_STAGES.length)
    expect(buildPipelineView(undefined, { running: true })[0].phase).toBe('active')
  })
})

describe('agentMeta / detail helpers', () => {
  it('resolves known agents and formats unknown ids', () => {
    expect(agentMeta('bull').label).toBe('Bull Agent')
    expect(agentMeta('custom_bot').label).toBe('Custom Bot')
  })

  it('detailEntries flattens objects and wraps primitives', () => {
    expect(detailEntries({ a: 1 })).toEqual([{ key: 'a', value: 1 }])
    expect(detailEntries(['x'])).toEqual([{ key: 'value', value: ['x'] }])
    expect(isEmptyDetail({})).toBe(true)
    expect(isEmptyDetail({ a: 1 })).toBe(false)
  })

  it('isEmptyDetail treats null and empty arrays as empty', () => {
    expect(isEmptyDetail(null)).toBe(true)
    expect(isEmptyDetail([])).toBe(true)
  })

  it('agentMeta falls back for supervisor', () => {
    expect(agentMeta('supervisor').call).toBe('orchestration')
  })
})
