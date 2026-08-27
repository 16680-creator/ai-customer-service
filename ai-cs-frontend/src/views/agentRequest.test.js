import test from 'node:test'
import assert from 'node:assert/strict'

import { buildAgentConfirmRequest, buildAgentRequest, nextAgentRunId } from './agentRequest.js'

test('includes the latest runId when continuing an agent conversation', () => {
  assert.deepEqual(buildAgentRequest(1787387874147, '20260812215541010002', 'run-123'), {
    sessionId: 1787387874147,
    input: '20260812215541010002',
    runId: 'run-123',
  })
})

test('omits runId when starting a new agent conversation', () => {
  assert.deepEqual(buildAgentRequest(1787387874147, '我要申请退款', null), {
    sessionId: 1787387874147,
    input: '我要申请退款',
  })
})

test('keeps a run active only while the agent is waiting for user input', () => {
  assert.equal(nextAgentRunId({ runId: 'run-123', needsUserInput: true }), 'run-123')
  assert.equal(nextAgentRunId({ runId: 'run-123', needsUserInput: false }), null)
})

test('uses the backend confirmation contract when confirming an action', () => {
  assert.deepEqual(buildAgentConfirmRequest(1787387874147, 'run-123', 'token-456'), {
    sessionId: 1787387874147,
    runId: 'run-123',
    token: 'token-456',
    decision: 'CONFIRM',
  })
})
