## Requirements
Stop people spamming the API.

## High-level design
Each server keeps a HashMap of user id to request count in memory. When a request comes
in, increment the count and if it is too high return an error.

## Scaling
Add more servers.

## Failure handling
If something goes wrong, restart the service.
