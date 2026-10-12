## Requirements
Create a short code for a long URL, redirect on visit, optional custom alias, optional
expiry, click counts. 100M new links a month, 10B redirects a month: roughly 100 reads
per write, so the read path is the design.

## API
`POST /links` returns the short code; `GET /{code}` redirects. 302 rather than 301:
a permanent redirect is cached by browsers and proxies, so later clicks never reach us,
which would make click counts impossible and a link impossible to disable later. The cost
is more traffic, which the edge cache absorbs.

## Data model
Key-value: code to URL, with the code as the primary key, plus `created_at`, `expires_at`
and `owner`. Every read is "give me the URL for this code", so modelling it as a
single-key lookup keeps reads cheap and shardable by code. 6 billion links at 5 years of
retention needs 6 base62 characters, which gives 56.8 billion combinations.

## High-level design
Codes come from a counter encoded in base62, with each writer pre-allocating a range of
10,000. A counter is unique by construction, so no collision check is needed before
insert, and the pre-allocated ranges keep the counter from becoming a per-request
bottleneck.

## Scaling
A cache in front of the store keyed by code: a small fraction of links take most of the
traffic, so hit rates are high for little memory. Read replicas serve cache misses.
Redirects are served from points of presence close to the user, since the response is
tiny and cacheable.

## Failure handling
A cache miss falls through to a replica, and a replica failure falls through to the
primary. Clicks are recorded by emitting an event asynchronously and aggregating it
outside the request path: a durable write per redirect would put the most expensive
operation in front of the cheapest, and counts can be eventually consistent.

## Trade-offs
Counter over random codes: random requires a lookup before every insert. The cost is that
codes are sequential and therefore guessable, so anything private needs an unguessable
code instead. Custom aliases are checked against a unique constraint and return 409 on
conflict rather than silently appending a number, because the alias is part of what the
caller asked for.
