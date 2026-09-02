#!/usr/bin/env sh
set -eu

curl --fail-with-body --request POST http://localhost:8080/property-mails \
  --header 'Content-Type: application/json' \
  --data '{"eventId":"inspection-204-2026-09-05","kind":"INSPECTION_REMINDER","tenantEmail":"tenant@example.com","tenantName":"Avery","propertyName":"Harbor House","reference":"INS-204","dueDate":"2026-09-05"}'
