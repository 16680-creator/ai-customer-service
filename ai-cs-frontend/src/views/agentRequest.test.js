import { describe, expect, it } from 'vitest'

import { buildAgentConfirmRequest, buildAgentRequest, nextAgentRunId } from './agentRequest.js'

describe('agentRequest：Agent 请求体契约', () => {
  it('继续对话时携带上一次的 runId', () => {
    expect(buildAgentRequest(1787387874147, '20260812215541010002', 'run-123')).toEqual({
      sessionId: 1787387874147,
      input: '20260812215541010002',
      runId: 'run-123',
    })
  })

  it('新对话不携带 runId 字段', () => {
    const request = buildAgentRequest(1787387874147, '我要申请退款', null)
    expect(request).toEqual({ sessionId: 1787387874147, input: '我要申请退款' })
    expect('runId' in request).toBe(false)
  })

  it('仅当后端要求补充输入时保留 runId', () => {
    expect(nextAgentRunId({ runId: 'run-123', needsUserInput: true })).toBe('run-123')
    expect(nextAgentRunId({ runId: 'run-123', needsUserInput: false })).toBeNull()
    expect(nextAgentRunId(undefined)).toBeNull()
  })

  it('确认动作使用后端约定的字段', () => {
    expect(buildAgentConfirmRequest(1787387874147, 'run-123', 'token-456')).toEqual({
      sessionId: 1787387874147,
      runId: 'run-123',
      token: 'token-456',
      decision: 'CONFIRM',
    })
  })
})
