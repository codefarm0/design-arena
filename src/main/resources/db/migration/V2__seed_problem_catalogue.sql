-- ============================================================================
-- SEED DATA - curated by hand, intended for human review.
--
-- Three problems, each with a statement, requirements, scale hints, a seven-dimension
-- rubric, and ten multiple-choice questions whose options all carry an explanation.
--
-- Correctness matters more than volume here: every answer key and explanation below is
-- a teaching statement, so a reviewer should read them as prose, not skim them as data.
--
-- Rows are inserted by looking up the parent by its natural key (problem slug, question
-- position) rather than by hardcoding generated ids, so this migration stays readable
-- and cannot be broken by identity-sequence differences between environments.
-- ============================================================================

---------------------------------------------------------------------------
-- Problem 1: Rate Limiter
---------------------------------------------------------------------------

INSERT INTO problem (slug, title, difficulty, blurb, statement_md, position) VALUES (
    'rate-limiter',
    'Design a Rate Limiter',
    'MEDIUM',
    'Throttle API traffic across a fleet of servers without a single point of failure.',
    'Design a service that limits how many requests each client can make to a public API, across a fleet of API servers.

The limiter sits in the request path of every call, so it has to be fast, and it has to agree with itself no matter which server handles the request. Decide where the limits are enforced, where the counters live, what the API tells a throttled client, and what happens when the counter store is unavailable.',
    1
);

INSERT INTO problem_requirement (problem_id, kind, position, text)
SELECT p.id, v.kind, v.position, v.text
FROM problem p
CROSS JOIN (VALUES
    ('FUNCTIONAL', 1, 'Limit requests per user and per API key'),
    ('FUNCTIONAL', 2, 'Support different limits per endpoint'),
    ('FUNCTIONAL', 3, 'Tell clients when they are throttled, and when they may retry'),
    ('FUNCTIONAL', 4, 'Allow limits to be changed without a deploy'),
    ('NON_FUNCTIONAL', 1, 'Adds very little latency to each request'),
    ('NON_FUNCTIONAL', 2, 'Highly available; no single point of failure'),
    ('NON_FUNCTIONAL', 3, 'Limits hold across every API server, not per server')
) AS v(kind, position, text)
WHERE p.slug = 'rate-limiter';

INSERT INTO problem_scale_hint (problem_id, position, text)
SELECT p.id, v.position, v.text
FROM problem p
CROSS JOIN (VALUES
    (1, '10M daily active users'),
    (2, '5k requests/sec at peak'),
    (3, 'Default limit 1000 requests/hour per API key')
) AS v(position, text)
WHERE p.slug = 'rate-limiter';

INSERT INTO rubric (problem_id) SELECT id FROM problem WHERE slug = 'rate-limiter';

INSERT INTO rubric_dimension (rubric_id, position, name, description, weight)
SELECT r.id, v.position, v.name, v.description, v.weight
FROM rubric r
JOIN problem p ON p.id = r.problem_id
CROSS JOIN (VALUES
    (1, 'Requirements & estimation', 'States the limits being enforced, the scope of a limit (user, key, endpoint), and does rough arithmetic on request volume and counter memory.', 1),
    (2, 'API & data model', 'Defines the check performed per request, the counter key structure, and the response contract for a throttled client.', 1),
    (3, 'High-level architecture', 'Places enforcement somewhere sensible, names the components in the request path, and explains how a decision is reached per request.', 2),
    (4, 'Scalability & performance', 'Keeps the hot path cheap, avoids a per-request bottleneck, and explains how the design holds at peak traffic.', 2),
    (5, 'Reliability & failure modes', 'Says what happens when the counter store is slow or down, and whether the system fails open or closed, with a reason.', 2),
    (6, 'Trade-offs & justification', 'Compares at least one alternative honestly and justifies the choice against the stated requirements.', 1),
    (7, 'Rate-limiter must-haves', 'The problem-specific points a staff-level answer is expected to cover.', 2)
) AS v(position, name, description, weight)
WHERE p.slug = 'rate-limiter';

INSERT INTO rubric_must_have (rubric_dimension_id, position, text)
SELECT d.id, v.position, v.text
FROM rubric_dimension d
JOIN rubric r ON r.id = d.rubric_id
JOIN problem p ON p.id = r.problem_id
CROSS JOIN (VALUES
    (1, 'Names a specific algorithm (token bucket, sliding window, leaky bucket) and says why it fits the burst behaviour required'),
    (2, 'Counters are shared across servers and updated atomically, so 20 servers do not each allow the full limit'),
    (3, 'Returns 429 with a Retry-After header rather than a generic error'),
    (4, 'States the behaviour when the counter store is unavailable, fail open or fail closed, and justifies it'),
    (5, 'Expires idle counter keys, so memory does not grow with the total user base')
) AS v(position, text)
WHERE p.slug = 'rate-limiter' AND d.position = 7;

INSERT INTO question (problem_id, position, text, type, category)
SELECT p.id, v.position, v.text, v.type, v.category
FROM problem p
CROSS JOIN (VALUES
    (1, 'Which algorithm allows short bursts while still enforcing an average rate?', 'SINGLE', 'ALGORITHM'),
    (2, 'Under a fixed window of 100 requests per minute, a client sends 100 requests at 11:59:59 and 100 more at 12:00:00. What is the problem?', 'SINGLE', 'ALGORITHM'),
    (3, 'Where should the limit be enforced for a public API served by many backend services?', 'SINGLE', 'SCALING'),
    (4, 'Your rate limiter runs on 20 API servers. Where should the request counters live so limits hold across all of them?', 'SINGLE', 'CONSISTENCY'),
    (5, 'Two requests for the same key arrive at the same moment on different servers. Each reads the counter, adds one, and writes it back. What is wrong?', 'SINGLE', 'CONSISTENCY'),
    (6, 'Which techniques genuinely reduce the latency the limiter adds to each request? Select all that apply.', 'MULTI', 'SCALING'),
    (7, 'What should the API return when a client exceeds its limit?', 'SINGLE', 'API DESIGN'),
    (8, 'Which response headers help a well-behaved client stay inside its limit? Select all that apply.', 'MULTI', 'API DESIGN'),
    (9, 'The counter store becomes unavailable during peak traffic. The API values availability over perfectly accurate limits. What is the best behaviour?', 'SINGLE', 'FAILURE HANDLING'),
    (10, 'You key counters as user_id:endpoint in a shared store. What keeps memory from growing with every user who has ever called the API?', 'SINGLE', 'STORAGE')
) AS v(position, text, type, category)
WHERE p.slug = 'rate-limiter';

INSERT INTO question_option (question_id, position, text, is_correct, explanation)
SELECT q.id, v.position, v.text, v.is_correct, v.explanation
FROM (VALUES
    -- Q1 algorithm choice
    (1, 1, 'Fixed window counter', FALSE, 'A fixed window is the simplest to implement, but it allows a double burst at the window boundary and gives no credit for idle time.'),
    (1, 2, 'Token bucket', TRUE, 'Tokens refill at a steady rate up to a cap, so saved tokens absorb a burst while the long-run average stays at the refill rate. This is exactly the required behaviour.'),
    (1, 3, 'Leaky bucket as a queue', FALSE, 'A leaky bucket smooths traffic to a constant outflow. That is useful for shaping, but it deliberately removes the bursts you were asked to allow.'),
    (1, 4, 'Sliding window log', FALSE, 'A log of timestamps per client is accurate and prevents boundary bursts, but it stores one entry per request and is the most expensive option in memory.'),
    -- Q2 fixed window boundary
    (2, 1, 'Nothing: each window saw exactly its allowed 100 requests', FALSE, 'Each window is within its limit when counted separately, which is precisely why the flaw is easy to miss. The problem is what the client achieved across the boundary.'),
    (2, 2, 'The client achieved 200 requests in one second, twice the intended rate', TRUE, 'Counters reset at the window edge, so a client that waits for the boundary can spend two full windows back to back. A sliding window or token bucket removes this.'),
    (2, 3, 'The second burst is rejected because the counter has not reset yet', FALSE, 'The counter resets exactly at the window boundary, so the second burst is admitted in full. That is the flaw.'),
    (2, 4, 'The counter overflows and the limit stops being enforced', FALSE, 'Nothing overflows: the count is small. The flaw is the reset behaviour, not arithmetic.'),
    -- Q3 enforcement point
    (3, 1, 'In each backend service, separately', FALSE, 'Every service then needs the same logic, and a client can still exhaust a shared resource by spreading calls across services. It also duplicates the work.'),
    (3, 2, 'At a shared entry point such as the API gateway, before requests reach backends', TRUE, 'One enforcement point sees all traffic for a client, protects every backend behind it, and keeps the logic in a single place that can be changed without touching services.'),
    (3, 3, 'In the client SDK, before the request is sent', FALSE, 'Client-side limiting is a courtesy, not a control: a client you do not control can simply not run it. It is useful as a politeness measure, never as enforcement.'),
    (3, 4, 'In the database, by rejecting queries beyond a quota', FALSE, 'By the time a request reaches the database it has already consumed connection, CPU and network. Limiting there protects the database but not the rest of the system.'),
    -- Q4 counter location
    (4, 1, 'In-memory counters on each server', FALSE, 'With 20 servers each holding its own count, a client can send up to 20 times the limit. Per-server memory only works if the limit itself is per server.'),
    (4, 2, 'A shared store such as Redis, updated with atomic increments', TRUE, 'One shared count, updated atomically, is what makes a limit hold across the fleet. The round trip costs latency, which is the price of a correct distributed count.'),
    (4, 3, 'A relational database row per user, updated on every request', FALSE, 'A row update per request turns the limiter into the busiest writer in the system, and row locks under contention add latency exactly when traffic is highest.'),
    (4, 4, 'At the client, using a signed token that tracks usage', FALSE, 'The client holds the state it is being limited by, so it can discard or replay the token. Anything a client can edit cannot be the source of truth.'),
    -- Q5 atomicity
    (5, 1, 'Nothing, the final value is correct either way', FALSE, 'Both servers read the same starting value, so one increment is lost. The final count is lower than the real number of requests.'),
    (5, 2, 'It is a lost update: both read the same value, so one increment disappears and the client gets extra requests', TRUE, 'Read-modify-write from two places is a classic race. An atomic INCR, or a small script that runs server-side in the store, makes the whole operation indivisible.'),
    (5, 3, 'The store rejects the second write because the value changed', FALSE, 'A plain write does not check what it is overwriting. You only get that behaviour if you ask for it, with a compare-and-set or a transaction.'),
    (5, 4, 'The counter becomes negative', FALSE, 'Nothing here subtracts. The defect is an undercount from a lost update.'),
    -- Q6 hot path latency (multi)
    (6, 1, 'Keep the limiter in the same network zone as the counter store', TRUE, 'Most of the cost of a counter check is the network round trip, so physical and network proximity is the single biggest lever.'),
    (6, 2, 'Allow a small local allowance per instance and reconcile with the shared store periodically', TRUE, 'Trading a little accuracy for a lot of latency is a legitimate design: each instance admits a few requests locally and syncs in the background.'),
    (6, 3, 'Do the counter check after the request has been served, and reject the next one', TRUE, 'Checking asynchronously takes the store off the hot path entirely. The cost is that a client can exceed the limit briefly before being cut off.'),
    (6, 4, 'Use a relational database with an index on the counter key', FALSE, 'An index makes a slow store less slow; it does not make a durable, disk-backed write per request fast enough for the hot path of every API call.'),
    -- Q7 throttled response
    (7, 1, '503 Service Unavailable', FALSE, '503 says the service is broken, so clients retry as if there were an outage and often make things worse. The service is fine, the client is over its limit.'),
    (7, 2, '429 Too Many Requests with a Retry-After header', TRUE, '429 names the real reason, and Retry-After tells the client exactly when to come back, so a well-behaved client backs off instead of hammering you.'),
    (7, 3, '403 Forbidden', FALSE, '403 means not allowed at all, which invites the client to stop and investigate credentials rather than retry later.'),
    (7, 4, '200 OK with an empty body', FALSE, 'Hiding a rejection behind a success code means every client treats throttling as valid data. Errors must be visible to be handled.'),
    -- Q8 headers (multi)
    (8, 1, 'Retry-After, in seconds or as a date', TRUE, 'The one header that turns a rejection into actionable information: it says when to try again.'),
    (8, 2, 'A header giving the limit that applies to this client', TRUE, 'Telling the client its ceiling lets it pace itself instead of discovering the limit by being rejected.'),
    (8, 3, 'A header giving how many requests remain in the current window', TRUE, 'Remaining quota lets a client slow down before it is throttled, which is the whole point of publishing limits.'),
    (8, 4, 'A header containing the internal counter key for the client', FALSE, 'Internal key structure helps nobody outside the system and tells an attacker how counters are addressed. Do not leak implementation detail in headers.'),
    -- Q9 store unavailable
    (9, 1, 'Reject every request until the store recovers', FALSE, 'Failing closed turns a counter outage into a full API outage. That is the right choice only when exceeding the limit is more costly than being down, such as billing or safety limits.'),
    (9, 2, 'Allow requests through, fall back to a conservative per-instance limit, and alert', TRUE, 'Failing open keeps the API serving while a degraded local limit still protects against the worst abuse, and the alert makes sure degraded mode is noticed. This matches the stated preference for availability.'),
    (9, 3, 'Allow every request through with no limit at all, and log it', FALSE, 'Failing fully open is an invitation: a single abusive client can take down the backends the limiter was protecting. Keep a coarse local limit.'),
    (9, 4, 'Buffer requests until the store is back, then replay them', FALSE, 'Buffering turns a counter outage into unbounded memory growth and delivers a stampede on recovery. Requests in a hot path should be decided now, not queued.'),
    -- Q10 key lifetime
    (10, 1, 'Set a TTL on each counter key so idle keys expire on their own', TRUE, 'The counter is only meaningful for the length of its window, so letting the store expire it bounds memory to the clients who are currently active.'),
    (10, 2, 'Run a nightly job that deletes keys for inactive users', FALSE, 'A cleanup job is extra moving parts doing badly what a TTL does precisely, and memory still peaks between runs.'),
    (10, 3, 'Store counters for every registered user up front', FALSE, 'Pre-creating keys allocates memory for users who may never call the API, which is the opposite of what is needed.'),
    (10, 4, 'Cap the number of keys and evict the oldest when full', FALSE, 'Eviction under pressure silently drops counters for active clients, which removes their limits at the moment traffic is highest.')
) AS v(question_position, position, text, is_correct, explanation)
JOIN problem p ON p.slug = 'rate-limiter'
JOIN question q ON q.problem_id = p.id AND q.position = v.question_position;

---------------------------------------------------------------------------
-- Problem 2: URL Shortener
---------------------------------------------------------------------------

INSERT INTO problem (slug, title, difficulty, blurb, statement_md, position) VALUES (
    'url-shortener',
    'Design a URL Shortener',
    'EASY',
    'Generate short links, redirect fast, and handle heavy read traffic.',
    'Design a service that turns a long URL into a short link and redirects anyone who visits that link to the original address.

The write path is small and the read path is enormous, which shapes almost every decision. Decide how short codes are generated, what guarantees uniqueness, how a redirect stays fast under heavy load, and how the service avoids becoming a tool for abuse.',
    2
);

INSERT INTO problem_requirement (problem_id, kind, position, text)
SELECT p.id, v.kind, v.position, v.text
FROM problem p
CROSS JOIN (VALUES
    ('FUNCTIONAL', 1, 'Create a short code for a long URL'),
    ('FUNCTIONAL', 2, 'Redirect a short code to its original URL'),
    ('FUNCTIONAL', 3, 'Support optional custom aliases'),
    ('FUNCTIONAL', 4, 'Support optional link expiry'),
    ('FUNCTIONAL', 5, 'Report click counts per link'),
    ('NON_FUNCTIONAL', 1, 'Redirects are fast, well under 100ms at the edge'),
    ('NON_FUNCTIONAL', 2, 'Short codes are never reused or reassigned'),
    ('NON_FUNCTIONAL', 3, 'Read traffic vastly exceeds write traffic')
) AS v(kind, position, text)
WHERE p.slug = 'url-shortener';

INSERT INTO problem_scale_hint (problem_id, position, text)
SELECT p.id, v.position, v.text
FROM problem p
CROSS JOIN (VALUES
    (1, '100M new links per month'),
    (2, '10B redirects per month'),
    (3, 'Read:write ratio around 100:1'),
    (4, 'Links retained for 5 years')
) AS v(position, text)
WHERE p.slug = 'url-shortener';

INSERT INTO rubric (problem_id) SELECT id FROM problem WHERE slug = 'url-shortener';

INSERT INTO rubric_dimension (rubric_id, position, name, description, weight)
SELECT r.id, v.position, v.name, v.description, v.weight
FROM rubric r
JOIN problem p ON p.id = r.problem_id
CROSS JOIN (VALUES
    (1, 'Requirements & estimation', 'Works out code length from the expected number of links, and estimates storage and read throughput from the given scale.', 2),
    (2, 'API & data model', 'Defines the create and redirect endpoints, the stored record, and the redirect status code with a reason.', 2),
    (3, 'High-level architecture', 'Separates the write path from the read path and names the components a redirect passes through.', 1),
    (4, 'Scalability & performance', 'Treats the service as read-heavy: caching, replicas or edge delivery, and a cheap lookup on the hot path.', 2),
    (5, 'Reliability & failure modes', 'Handles code collisions, cache misses, and the behaviour when a datastore or cache is unavailable.', 1),
    (6, 'Trade-offs & justification', 'Compares code-generation strategies and caching choices honestly against the requirements.', 1),
    (7, 'URL-shortener must-haves', 'The problem-specific points a staff-level answer is expected to cover.', 2)
) AS v(position, name, description, weight)
WHERE p.slug = 'url-shortener';

INSERT INTO rubric_must_have (rubric_dimension_id, position, text)
SELECT d.id, v.position, v.text
FROM rubric_dimension d
JOIN rubric r ON r.id = d.rubric_id
JOIN problem p ON p.id = r.problem_id
CROSS JOIN (VALUES
    (1, 'Derives the short-code length from the number of links the service must hold, rather than picking a length arbitrarily'),
    (2, 'Explains what guarantees uniqueness: a unique constraint, a pre-allocated key range, or a collision check with retry'),
    (3, 'Puts a cache in front of the lookup and says what happens on a cache miss'),
    (4, 'Chooses a redirect status code deliberately and explains the caching consequence'),
    (5, 'Addresses abuse: validating the target URL and preventing the service from being used to mask malicious links')
) AS v(position, text)
WHERE p.slug = 'url-shortener' AND d.position = 7;

INSERT INTO question (problem_id, position, text, type, category)
SELECT p.id, v.position, v.text, v.type, v.category
FROM problem p
CROSS JOIN (VALUES
    (1, 'The service must hold about 6 billion links. Using a base62 alphabet, what is the shortest code length that fits?', 'SINGLE', 'ESTIMATION'),
    (2, 'Which short-code generation strategy avoids collisions entirely without a lookup before insert?', 'SINGLE', 'ALGORITHM'),
    (3, 'You generate codes by hashing the long URL and truncating the hash. What must you still handle?', 'SINGLE', 'ALGORITHM'),
    (4, 'Which redirect status code should a short link return by default, given that click counts matter?', 'SINGLE', 'API DESIGN'),
    (5, 'With a 100:1 read:write ratio, what has the largest effect on redirect latency?', 'SINGLE', 'SCALING'),
    (6, 'Which choices suit the read-heavy redirect path? Select all that apply.', 'MULTI', 'SCALING'),
    (7, 'What is the right storage model for the link record itself?', 'SINGLE', 'STORAGE'),
    (8, 'A user requests the custom alias "launch", which already exists. What should the API do?', 'SINGLE', 'API DESIGN'),
    (9, 'Click counting must not slow down redirects. What is the best approach?', 'SINGLE', 'FAILURE HANDLING'),
    (10, 'Which safeguard matters most to stop the shortener becoming a tool for abuse?', 'SINGLE', 'SECURITY')
) AS v(position, text, type, category)
WHERE p.slug = 'url-shortener';

INSERT INTO question_option (question_id, position, text, is_correct, explanation)
SELECT q.id, v.position, v.text, v.is_correct, v.explanation
FROM (VALUES
    -- Q1 code length
    (1, 1, '5 characters', FALSE, 'Base62 with 5 characters gives about 916 million combinations, well short of 6 billion.'),
    (1, 2, '6 characters', TRUE, 'Base62 with 6 characters gives about 56.8 billion combinations, comfortably above 6 billion with room for sparse allocation. Deriving the length from the count is the point of the question.'),
    (1, 3, '8 characters', FALSE, '8 characters work, but they are longer than needed. In a product whose whole value is shortness, spending two extra characters without reason is a real cost.'),
    (1, 4, '12 characters', FALSE, 'This is far more space than required and defeats the purpose of a short link.'),
    -- Q2 generation strategy
    (2, 1, 'Random codes, checking the database for a collision before insert', FALSE, 'This works and is common, but it requires a lookup before every insert, which is exactly what the question excludes.'),
    (2, 2, 'A unique counter encoded in base62, with ranges handed out to each writer', TRUE, 'A counter is unique by construction, so no check is needed. Handing each writer a pre-allocated range keeps the counter from becoming a per-request bottleneck.'),
    (2, 3, 'A truncated hash of the long URL', FALSE, 'Truncating a hash reintroduces collisions: two different URLs can share a prefix, so you are back to checking before insert.'),
    (2, 4, 'The current timestamp in milliseconds, encoded in base62', FALSE, 'Two links created in the same millisecond collide, and timestamps are both predictable and longer than necessary.'),
    -- Q3 hashing caveat
    (3, 1, 'Nothing: a cryptographic hash never collides', FALSE, 'The full hash is effectively collision-free, but you are not storing the full hash. Truncation is what creates collisions.'),
    (3, 2, 'Collisions between different URLs that share the truncated prefix, detected on insert and retried', TRUE, 'Truncation shrinks the output space, so collisions are expected at scale. A unique constraint plus a retry with a different salt or length handles it correctly.'),
    (3, 3, 'Reversing the hash to recover the original URL', FALSE, 'You never need to reverse it: the mapping is stored. Reversibility is not a requirement here.'),
    (3, 4, 'Codes becoming longer as more links are added', FALSE, 'A truncated hash has a fixed length by definition. Growth is not the issue; collisions are.'),
    -- Q4 redirect status
    (4, 1, '301 Moved Permanently', FALSE, '301 is cached aggressively by browsers and proxies, so later clicks never reach your service. That is fast, but it destroys click counting and makes a link impossible to change or disable.'),
    (4, 2, '302 Found', TRUE, 'A temporary redirect keeps the client coming back, so clicks are countable and a link can later be expired or retargeted. The cost is more traffic, which caching at your edge absorbs.'),
    (4, 3, '307 Temporary Redirect with a POST-preserving body', FALSE, '307 preserves the method, which matters for form submissions but not for a link click. It is not wrong, but it solves a problem this service does not have.'),
    (4, 4, '200 OK with an HTML meta refresh', FALSE, 'This hides the redirect from clients and crawlers, adds a page load, and breaks link previews. Use the protocol feature built for this.'),
    -- Q5 latency driver
    (5, 1, 'The speed of the code-generation algorithm', FALSE, 'Generation happens once per link on the write path. It has no bearing on the latency of the 100 reads that follow.'),
    (5, 2, 'Whether the lookup is served from cache or from the database', TRUE, 'The redirect is a single key lookup repeated billions of times, so where that lookup is answered from dominates everything else. A cache hit is a memory read; a miss is a disk read plus network.'),
    (5, 3, 'The size of the long URL being stored', FALSE, 'URLs are small, and a few hundred bytes makes no measurable difference to lookup time.'),
    (5, 4, 'Using a relational database rather than a document store', FALSE, 'Either can serve a primary-key lookup quickly. The engine matters far less than whether you reach it at all.'),
    -- Q6 read-heavy choices (multi)
    (6, 1, 'A cache in front of the datastore, keyed by short code', TRUE, 'Hot links are a small fraction of all links and take most of the traffic, so a cache absorbs the bulk of reads with little memory.'),
    (6, 2, 'Read replicas to spread lookups that miss the cache', TRUE, 'Misses still need the datastore, and replicas scale that path without touching the writer.'),
    (6, 3, 'Serving redirects from points of presence close to users', TRUE, 'The redirect is tiny and cacheable, so answering it near the user removes most of the round-trip time.'),
    (6, 4, 'Writing every click synchronously to the link row before redirecting', FALSE, 'This puts a write in front of every read, turning the cheapest path in the system into the most expensive and contended one.'),
    -- Q7 storage model
    (7, 1, 'A key-value lookup from short code to URL, with the code as the primary key', TRUE, 'Every read is "give me the URL for this code", so the access pattern is a single-key lookup. Modelling the store to match it keeps reads cheap and shardable by code.'),
    (7, 2, 'A normalised relational schema with the URL split into host, path and query tables', FALSE, 'Splitting the URL adds joins to a path that only ever needs the whole string back. Normalisation here buys nothing and costs latency.'),
    (7, 3, 'A search index over the long URLs', FALSE, 'Nobody searches by long URL on the redirect path. An index would be a second system to maintain for a query the product does not need.'),
    (7, 4, 'An append-only log read from the end on each request', FALSE, 'A log is excellent for recording events and terrible for random single-key reads, which is the one access pattern that matters here.'),
    -- Q8 alias conflict
    (8, 1, 'Return 409 Conflict and let the caller choose another alias', TRUE, 'The alias is part of what the caller asked for, so silently changing it would be wrong. 409 states the problem precisely and leaves the decision with the caller.'),
    (8, 2, 'Silently append a number to make it unique', FALSE, 'The caller now has a link they did not ask for and may not notice. Never quietly alter the identifier a caller requested.'),
    (8, 3, 'Overwrite the existing link with the new target', FALSE, 'This hijacks somebody else''s live link. Short codes must never be reassigned.'),
    (8, 4, 'Return 500 Internal Server Error', FALSE, 'Nothing broke; the request simply cannot be satisfied. A 5xx tells the caller to retry an operation that will never succeed.'),
    -- Q9 click counting
    (9, 1, 'Increment a counter in the link row inside the redirect request', FALSE, 'A durable write per redirect is the most expensive thing you could add to the hot path, and row contention on popular links makes it worse.'),
    (9, 2, 'Emit a click event asynchronously and aggregate it outside the request path', TRUE, 'The redirect returns immediately and counting happens elsewhere. Counts become eventually consistent, which for analytics is an acceptable trade.'),
    (9, 3, 'Parse the web server access logs once a month', FALSE, 'Logs are a valid source, but a month of latency makes the number useless to the person who shared the link.'),
    (9, 4, 'Count in memory on each server and never persist', FALSE, 'Counts vanish on restart and are split across servers, so the total is never correct.'),
    -- Q10 abuse
    (10, 1, 'Validating and screening the target URL, and being able to disable a link after the fact', TRUE, 'A shortener hides the destination, which is exactly what makes it attractive for phishing. Screening on create plus the ability to kill a live link is the control that matters.'),
    (10, 2, 'Making short codes long enough to be unguessable', FALSE, 'Unguessable codes stop enumeration of other people''s links, which is worth doing, but they do nothing about a malicious link the attacker created and is sharing deliberately.'),
    (10, 3, 'Requiring a login before redirecting', FALSE, 'This breaks the product: links are shared with people who have no account. Friction on the read path is not an abuse control.'),
    (10, 4, 'Storing the creator''s IP address', FALSE, 'Useful for investigation after the fact, but it neither prevents the abuse nor protects the person who clicks the link.')
) AS v(question_position, position, text, is_correct, explanation)
JOIN problem p ON p.slug = 'url-shortener'
JOIN question q ON q.problem_id = p.id AND q.position = v.question_position;

---------------------------------------------------------------------------
-- Problem 3: Notification System
---------------------------------------------------------------------------

INSERT INTO problem (slug, title, difficulty, blurb, statement_md, position) VALUES (
    'notification-system',
    'Design a Notification System',
    'HARD',
    'Send email, SMS and push at scale with retries, preferences and rate limits.',
    'Design a service other teams call to notify a user, over email, SMS or mobile push.

The service owns everything between "this team wants to tell this user something" and the message arriving: choosing channels from user preferences, handing off to third-party providers, retrying what fails, and never sending the same thing twice. Decide the API, how the work is queued, what makes delivery reliable, and how one noisy sender cannot starve everyone else.',
    3
);

INSERT INTO problem_requirement (problem_id, kind, position, text)
SELECT p.id, v.kind, v.position, v.text
FROM problem p
CROSS JOIN (VALUES
    ('FUNCTIONAL', 1, 'Send over email, SMS and mobile push from one API'),
    ('FUNCTIONAL', 2, 'Respect per-user channel preferences and opt-outs'),
    ('FUNCTIONAL', 3, 'Retry transient provider failures'),
    ('FUNCTIONAL', 4, 'Never deliver the same notification twice'),
    ('FUNCTIONAL', 5, 'Report delivery status back to the calling team'),
    ('NON_FUNCTIONAL', 1, 'A slow or failing provider must not block other channels'),
    ('NON_FUNCTIONAL', 2, 'One noisy sender must not starve the others'),
    ('NON_FUNCTIONAL', 3, 'Accepting a send request is fast; delivery is asynchronous')
) AS v(kind, position, text)
WHERE p.slug = 'notification-system';

INSERT INTO problem_scale_hint (problem_id, position, text)
SELECT p.id, v.position, v.text
FROM problem p
CROSS JOIN (VALUES
    (1, '50M notifications per day'),
    (2, '10x spike during campaign sends'),
    (3, 'Three external providers, each with its own rate limit'),
    (4, 'Delivery status retained for 90 days')
) AS v(position, text)
WHERE p.slug = 'notification-system';

INSERT INTO rubric (problem_id) SELECT id FROM problem WHERE slug = 'notification-system';

INSERT INTO rubric_dimension (rubric_id, position, name, description, weight)
SELECT r.id, v.position, v.name, v.description, v.weight
FROM rubric r
JOIN problem p ON p.id = r.problem_id
CROSS JOIN (VALUES
    (1, 'Requirements & estimation', 'Separates the accept path from the delivery path, and sizes queue throughput and storage from the stated volumes.', 1),
    (2, 'API & data model', 'Defines the send request, the idempotency key, the notification and delivery-attempt records, and how status is exposed.', 2),
    (3, 'High-level architecture', 'Uses a queue between accept and delivery, isolates channels, and abstracts providers behind one interface per channel.', 2),
    (4, 'Scalability & performance', 'Handles a 10x campaign spike, per-provider rate limits, and fair sharing between senders.', 2),
    (5, 'Reliability & failure modes', 'Retries with backoff, uses a dead-letter path, and prevents duplicate delivery under retry.', 2),
    (6, 'Trade-offs & justification', 'Justifies queue choice, at-least-once versus exactly-once handling, and ordering guarantees.', 1),
    (7, 'Notification must-haves', 'The problem-specific points a staff-level answer is expected to cover.', 2)
) AS v(position, name, description, weight)
WHERE p.slug = 'notification-system';

INSERT INTO rubric_must_have (rubric_dimension_id, position, text)
SELECT d.id, v.position, v.text
FROM rubric_dimension d
JOIN rubric r ON r.id = d.rubric_id
JOIN problem p ON p.id = r.problem_id
CROSS JOIN (VALUES
    (1, 'Accepts the request, persists it, and returns immediately, with delivery happening asynchronously'),
    (2, 'Uses a caller-supplied idempotency key so a retried API call does not produce a second message'),
    (3, 'Retries transient failures with exponential backoff and jitter, with a bounded attempt count and a dead-letter path'),
    (4, 'Isolates channels and providers so one failing provider cannot stall the others'),
    (5, 'Checks user preferences and opt-outs before sending, and treats opt-out as non-negotiable'),
    (6, 'Rate limits per sender and per provider, so a campaign cannot starve transactional sends')
) AS v(position, text)
WHERE p.slug = 'notification-system' AND d.position = 7;

INSERT INTO question (problem_id, position, text, type, category)
SELECT p.id, v.position, v.text, v.type, v.category
FROM problem p
CROSS JOIN (VALUES
    (1, 'A team calls your API to notify a user. What should the API do before responding?', 'SINGLE', 'ARCHITECTURE'),
    (2, 'Why put a queue between accepting a notification and delivering it?', 'SINGLE', 'ARCHITECTURE'),
    (3, 'The SMS provider starts timing out. Email and push are healthy. What design prevents SMS from stalling the others?', 'SINGLE', 'FAILURE HANDLING'),
    (4, 'A provider call fails with a 503. What is the correct retry strategy?', 'SINGLE', 'FAILURE HANDLING'),
    (5, 'Your queue guarantees at-least-once delivery, so a worker may process the same message twice. How do you avoid sending twice?', 'SINGLE', 'CONSISTENCY'),
    (6, 'A marketing campaign enqueues 20 million notifications. Transactional sends such as password resets must still go out within seconds. What do you do?', 'SINGLE', 'SCALING'),
    (7, 'Which responsibilities belong behind a per-channel provider interface? Select all that apply.', 'MULTI', 'ARCHITECTURE'),
    (8, 'Where should user channel preferences and opt-outs be checked?', 'SINGLE', 'API DESIGN'),
    (9, 'A notification has exhausted its retries. What should happen to it?', 'SINGLE', 'FAILURE HANDLING'),
    (10, 'How should the calling team learn whether a notification was delivered?', 'SINGLE', 'API DESIGN')
) AS v(position, text, type, category)
WHERE p.slug = 'notification-system';

INSERT INTO question_option (question_id, position, text, is_correct, explanation)
SELECT q.id, v.position, v.text, v.is_correct, v.explanation
FROM (VALUES
    -- Q1 accept path
    (1, 1, 'Call the email or SMS provider and return the provider response', FALSE, 'The caller now waits on a third party you do not control, and a provider outage becomes an outage in their request path. Never put an external dependency inside a synchronous accept.'),
    (1, 2, 'Validate the request, persist it, enqueue it, and return an accepted response with an id', TRUE, 'The caller gets a fast, durable acknowledgement and an id to track. Persisting before enqueueing means an accepted notification is never lost if a worker or broker restarts.'),
    (1, 3, 'Hold the request in memory and return immediately', FALSE, 'Returning success for work that exists only in memory means a restart silently loses accepted notifications. An acknowledgement must be backed by durable state.'),
    (1, 4, 'Return the delivery status once every channel has confirmed', FALSE, 'Confirmation can take seconds to minutes and depends on carriers and devices. A synchronous API cannot wait for it.'),
    -- Q2 why a queue
    (2, 1, 'It absorbs spikes and lets delivery be retried independently of the caller', TRUE, 'The queue decouples a fast, bursty accept path from a slow, failure-prone delivery path, and it is what makes retries possible without the caller being involved.'),
    (2, 2, 'It makes delivery faster than calling the provider directly', FALSE, 'A queue adds a hop, so a single notification is slightly slower. What it buys is throughput, isolation and retriability, not latency.'),
    (2, 3, 'It guarantees notifications are delivered exactly once', FALSE, 'Queues generally guarantee at-least-once. Exactly-once delivery is achieved by making the consumer idempotent, not by the queue.'),
    (2, 4, 'It removes the need to persist the notification', FALSE, 'A broker is a transport, not your system of record. You still need the notification stored to report status and to recover.'),
    -- Q3 isolation
    (3, 1, 'Separate queues and worker pools per channel, with a circuit breaker around each provider', TRUE, 'Shared capacity is how one slow dependency becomes everyone''s problem. Per-channel queues and pools bound the damage, and a breaker stops hammering a provider that is already failing.'),
    (3, 2, 'One queue for everything, with SMS messages given lower priority', FALSE, 'Priority does not help: slow SMS calls still occupy workers from the shared pool, so email and push starve behind them.'),
    (3, 3, 'Increase the timeout on SMS calls so they eventually succeed', FALSE, 'A longer timeout holds each worker hostage for longer, which makes the starvation worse rather than better.'),
    (3, 4, 'Drop SMS notifications while the provider is slow', FALSE, 'Dropping accepted work loses notifications the caller was told would be sent. Park them for retry instead.'),
    -- Q4 retry strategy
    (4, 1, 'Retry immediately in a tight loop until it succeeds', FALSE, 'This adds load to a service that is already struggling and can keep it down. It also has no bound, so one message can occupy a worker indefinitely.'),
    (4, 2, 'Retry with exponential backoff and jitter, up to a bounded number of attempts', TRUE, 'Backoff gives the provider room to recover, jitter stops every worker retrying in lockstep, and a bound means a permanently broken message eventually stops consuming capacity.'),
    (4, 3, 'Fail permanently on the first error', FALSE, 'A 503 is usually transient, so giving up immediately throws away notifications that would succeed seconds later.'),
    (4, 4, 'Retry at a fixed one-second interval forever', FALSE, 'A fixed interval synchronises retries into a thundering herd, and retrying forever means a bad message is never cleared out.'),
    -- Q5 idempotency
    (5, 1, 'Rely on the queue to deduplicate messages', FALSE, 'The question states the queue is at-least-once. Expecting the transport to solve a problem it does not claim to solve is how duplicates reach users.'),
    (5, 2, 'Record a unique key per notification and channel, and claim it before sending', TRUE, 'A uniqueness constraint on the key makes the second attempt lose the race and skip the send. The deduplication lives in your own durable state, where you control it.'),
    (5, 3, 'Check whether a similar notification was sent recently', FALSE, '"Similar" and "recently" are both fuzzy, so this suppresses legitimate sends and still lets duplicates through. Deduplication needs an exact key.'),
    (5, 4, 'Delete the message from the queue before sending it', FALSE, 'Acknowledging before the work is done converts a duplicate into a lost notification: a crash mid-send leaves nothing to retry.'),
    -- Q6 fairness
    (6, 1, 'Separate the campaign traffic from transactional traffic, with its own queue and capacity limits', TRUE, 'These are different classes of work with different urgency. Isolating them, and capping what the bulk class may consume, is what keeps a password reset fast during a campaign.'),
    (6, 2, 'Scale the worker pool until the backlog clears', FALSE, 'More workers push harder against provider rate limits, and transactional messages still queue behind 20 million others in the same queue.'),
    (6, 3, 'Process the queue in strict arrival order', FALSE, 'Strict ordering is exactly the problem: it puts every urgent message behind the whole campaign.'),
    (6, 4, 'Reject the campaign request', FALSE, 'Bulk sending is a legitimate use of the service. The job of the design is to carry it without harming other senders.'),
    -- Q7 provider interface (multi)
    (7, 1, 'Translating the internal message into the provider request format', TRUE, 'Format translation is provider-specific by definition, and keeping it behind the interface is what lets a provider be swapped without touching the rest of the system.'),
    (7, 2, 'Classifying a provider error as retryable or permanent', TRUE, 'Only the adapter knows what a given provider''s error codes mean. It should return a normalised outcome so the retry logic stays generic.'),
    (7, 3, 'Respecting the provider''s own rate limits', TRUE, 'Each provider has its own ceiling, so the limit belongs with the adapter that knows it rather than being duplicated in every caller.'),
    (7, 4, 'Deciding which channels a user has opted into', FALSE, 'Preferences are a property of the user, not of a provider. Putting that decision in an adapter means it has to be reimplemented for each one.'),
    -- Q8 preference check
    (8, 1, 'In the calling team''s code, before they call your API', FALSE, 'Every caller would reimplement it, and one mistake means a user who opted out gets a message. A rule that must always hold cannot be optional for callers.'),
    (8, 2, 'Once, in the notification service, before a channel send is enqueued', TRUE, 'One owner, one implementation, applied to every sender. Checking before enqueueing also avoids doing work for a channel that will never be used.'),
    (8, 3, 'In the provider adapter, immediately before the API call', FALSE, 'This is late enough that the work is already queued and duplicated across every adapter. It is also the hot path.'),
    (8, 4, 'In a nightly reconciliation job', FALSE, 'A message already sent cannot be unsent. Opt-out has to be enforced before delivery, not audited afterwards.'),
    -- Q9 exhausted retries
    (9, 1, 'Move it to a dead-letter queue, mark it failed, and alert if the rate is abnormal', TRUE, 'The message stops consuming capacity, the evidence is kept for diagnosis and replay, and the alert is what turns a silent failure into a known one.'),
    (9, 2, 'Discard it silently', FALSE, 'The caller was told the notification was accepted. Dropping it without a trace means nobody can find out what happened.'),
    (9, 3, 'Put it back on the main queue to try again later', FALSE, 'This is an unbounded loop by another name: the message cycles forever and competes with healthy traffic.'),
    (9, 4, 'Send it over a different channel instead', FALSE, 'Channel fallback can be a deliberate product decision, but it must be chosen explicitly, not be the default response to a failure.'),
    -- Q10 status reporting
    (10, 1, 'A status endpoint keyed by the notification id, plus webhooks for status changes', TRUE, 'Delivery status arrives asynchronously from providers, so it needs somewhere to be read and a push for callers who do not want to poll.'),
    (10, 2, 'In the response to the original send call', FALSE, 'At that moment nothing has been delivered yet. The only honest answer the accept call can give is "accepted".'),
    (10, 3, 'By having callers query the notification database directly', FALSE, 'That makes your schema everyone''s API and prevents you from ever changing it. Status belongs behind an interface.'),
    (10, 4, 'In a daily report emailed to each team', FALSE, 'Far too slow to be actionable, and unusable by code.')
) AS v(question_position, position, text, is_correct, explanation)
JOIN problem p ON p.slug = 'notification-system'
JOIN question q ON q.problem_id = p.id AND q.position = v.question_position;
