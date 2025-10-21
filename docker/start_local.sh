docker compose -p ticketmaster up -d

# await 15s
echo "Waiting for 15 seconds..."
sleep 15
echo "Done waiting."

## Create Secret Manager
aws --endpoint-url=http://localhost:4566 sqs create-queue \
  --queue-name check-booking-pending-state


aws sqs receive-message \
  --endpoint-url=http://localhost:4566 \
  --queue-url http://localhost.localstack.cloud:4566/000000000000/check-booking-pending-state \
  --max-number-of-messages 10 \
  --visibility-timeout 60 \
  --attribute-names All \
  --message-attribute-names All