export function buildAgentRequest(sessionId, input, runId) {
  const request = { sessionId, input }
  if (runId) request.runId = runId
  return request
}

export function nextAgentRunId(result) {
  return result?.needsUserInput && result.runId ? result.runId : null
}

export function buildAgentConfirmRequest(sessionId, runId, token) {
  return { sessionId, runId, token, decision: 'CONFIRM' }
}
