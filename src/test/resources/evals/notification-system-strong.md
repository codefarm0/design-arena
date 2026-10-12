## Requirements
One API for email, SMS and push. Respect per-user channel preferences and opt-outs. Retry
transient provider failures. Never deliver the same notification twice. Report delivery
status back to the calling team. 50M notifications a day with 10x campaign spikes, three
providers each with their own rate limit.

## API
`POST /notifications` takes a user id, a template id, variables and a caller-supplied
idempotency key. It validates, persists, enqueues and returns 202 with a notification id.
It never calls a provider inline: that would put a third party we do not control inside
the caller's request path. Status is read from `GET /notifications/{id}` or pushed by
webhook, because delivery status arrives asynchronously from providers.

## Data model
`notification` (id, user, template, idempotency key, created_at), `delivery_attempt`
(notification, channel, provider, status, attempt number, error). A unique constraint on
(idempotency key, channel) is what makes delivery exactly-once in practice: the queue is
at-least-once, so the consumer claims the key before sending and the second attempt loses
the race.

## High-level design
Accept, persist, enqueue. Separate queues and worker pools per channel so a slow SMS
provider cannot occupy the workers that email and push need. Each channel has a provider
adapter behind one interface: it translates the message, classifies errors as retryable
or permanent, and respects that provider's rate limit. Preferences and opt-outs are
checked once, in the notification service, before anything is enqueued — not in each
adapter and certainly not in each calling team's code.

## Scaling
Campaign traffic gets its own queue and a cap on the share of workers it may consume, so
a 20M campaign cannot delay a password reset. Workers scale horizontally up to each
provider's rate limit, which is enforced in the adapter rather than by hope.

## Failure handling
Transient failures retry with exponential backoff and jitter — jitter so workers do not
retry in lockstep — up to a bounded attempt count. After that the message goes to a
dead-letter queue, is marked failed, and an alert fires if the failure rate is abnormal.
A circuit breaker around each provider stops hammering one that is already down.

## Trade-offs
At-least-once delivery with an idempotent consumer, rather than chasing exactly-once in
the transport: exactly-once is a property of the consumer, not the queue. Per-channel
isolation costs more queues and more operational surface, which is worth it because the
alternative is one slow provider degrading everything.
