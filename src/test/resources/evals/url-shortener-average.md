## Requirements
Shorten URLs and redirect users to the original link. Track clicks.

## API
POST to create a link and GET on the short code to be redirected.

## Data model
A table with id, short code, long URL and a click count.

## High-level design
Generate a random 7-character code, check whether it already exists in the database and
retry if it does. Store the mapping and redirect with a 301 on lookup.

## Scaling
Put the service behind a load balancer and add a cache in front of the database for
popular links.

## Failure handling
If the database is unavailable the service returns a 500.

## Trade-offs
Random codes are easy to generate and hard to guess.
