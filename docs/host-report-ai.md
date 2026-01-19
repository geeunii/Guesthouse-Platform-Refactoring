# Host Report AI Summary

## Endpoints

### POST `/api/host/reports/reviews/ai-summary`
- **Auth**: ROLE_HOST only
- **Errors**: 401/403 when unauthenticated or non-host
  - **OpenAI 실패**: 502 (provider=openai + 실패 시)
- **Request**
```json
{
  "accommodationId": 123,
  "from": "2025-01-01",
  "to": "2025-01-31"
}
```
- **Response**
```json
{
  "accommodationId": 123,
  "from": "2025-01-01",
  "to": "2025-01-31",
  "generatedAt": "2025-01-31T13:45:10+09:00[Asia/Seoul]",
  "overview": ["...", "...", "..."],
  "positives": ["...", "...", "..."],
  "negatives": ["...", "...", "..."],
  "actions": ["...", "...", "..."],
  "risks": ["...", "..."]
}
```

## Feature Flag / Env

Default provider is **mock**.

```
ai.summary.provider=mock|openai
AI_SUMMARY_PROVIDER=mock|openai
OPENAI_API_KEY=...
OPENAI_MODEL=gpt-4o-mini
OPENAI_BASE_URL=https://api.openai.com/v1
ai.summary.connect-timeout-ms=5000
ai.summary.read-timeout-ms=8000
AI_SUMMARY_CONNECT_TIMEOUT_MS=5000
AI_SUMMARY_READ_TIMEOUT_MS=8000
```

## Notes
- Request/response only uses aggregated stats + recent reviews. No logs should include raw review content.
- If OpenAI is not configured or fails, server falls back to mock summary.
