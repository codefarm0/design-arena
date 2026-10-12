## High-level design
When something happens, call the email provider's API directly from the service that
needs to send the notification. For SMS, do the same with the SMS provider.

## Scaling
The providers handle the scaling for us.

## Failure handling
Log the error if sending fails.
