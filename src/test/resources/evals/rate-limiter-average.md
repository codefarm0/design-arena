## Requirements
Limit requests per user so nobody can overload the API.

## API
Middleware in front of the API checks the limit and returns 429 when the client is over.

## Data model
A Redis key per user holding a counter, incremented on each request.

## High-level design
Use a fixed window of one minute. On each request, increment the counter for the current
window and reject if it is above the limit. Redis is shared by all API servers so the
count is consistent.

## Scaling
Redis is fast and can handle the traffic. Add more API servers behind the load balancer
as needed.

## Failure handling
If Redis is down, allow the requests through so the API stays up.

## Trade-offs
A fixed window is simple to implement and understand, which is why I chose it.
