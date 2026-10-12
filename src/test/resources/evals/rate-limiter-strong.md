## Requirements
Per-API-key limits, configurable per endpoint, enforced across all 20 API servers.
Default 1000 requests/hour. At 5k RPS peak the limiter is on the hot path of every call,
so its added latency has to stay under about 1ms.

## API
A filter at the API gateway, ahead of every backend. On rejection: 429 with
`Retry-After`, plus `X-RateLimit-Limit` and `X-RateLimit-Remaining` on every response so
a well-behaved client can pace itself instead of discovering the limit by being refused.

## Data model
Redis key `rl:{api_key}:{endpoint}` holding `tokens` and `last_refill_ms`, with a TTL of
two refill windows so idle keys expire themselves. 10M keys at roughly 100 bytes is about
1GB, which fits in memory with room to spare.

## High-level design
Token bucket, because it allows short bursts while holding the average rate, which is
what an API wants. Refill is computed lazily on read rather than by a background job. The
check-and-decrement runs as a single Lua script inside Redis, so the read-modify-write is
atomic and two servers cannot both spend the last token.

## Scaling
Redis Cluster sharded by key, so the limiter scales horizontally with traffic. The
limiter sits in the same availability zone as its Redis shard to keep the round trip
local. Each gateway holds a short-lived local allowance of a few tokens and reconciles
asynchronously, which removes most of the hot-path round trips at the cost of a small,
bounded overshoot.

## Failure handling
If Redis is unavailable, fail open: fall back to a conservative per-instance limit
(1/20th of the global limit) and alert. This API values availability over perfectly
accurate limits, and a counter outage should not become an API outage. Failing closed
would be the right call for a billing or safety limit, and that is a per-endpoint
decision rather than a global one.

## Trade-offs
Token bucket over sliding-window log: the log is more accurate but stores one entry per
request, which at 5k RPS is far more memory for a precision nobody asked for. Local
allowance over strict correctness: a client can briefly exceed its limit by the size of
the allowance, which is acceptable when the alternative is a Redis round trip on every
request.
