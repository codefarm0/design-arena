## Requirements
Send notifications by email, SMS and push. Let users turn off channels they do not want.

## API
A POST endpoint that takes a user and a message and sends the notification.

## Data model
A notifications table with the user, the channel, the message and a status column.

## High-level design
The API writes the notification to a queue. Workers read from the queue and call the
right provider for the channel. The status column is updated when the provider responds.

## Scaling
Run more workers when the queue gets long.

## Failure handling
If the provider call fails, retry a few times before marking the notification as failed.

## Trade-offs
A queue decouples sending from the API so the API stays fast.
