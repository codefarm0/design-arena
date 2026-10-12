## High-level design
Store the URLs in a table. When someone creates a link, save the long URL and return the
row id as the short code. When someone visits, look up the id and redirect.

## Scaling
Add an index on the id column.

## Failure handling
Use try/catch around the database calls.
