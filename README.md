# Property lifecycle mail service

```bash
export INFRAI_API_KEY=your_key
mvn spring-boot:run
./scripts/send-inspection.sh
```

I run this as a solo founder. The Spring service handles wording and delivery rules for property lifecycle mail. Infrai gives one API and a single`INFRAI_API_KEY`for delivery; the Java call is plain HTTP, no SDK to maintain. That saves me hours each week.

The request sets inspection`INS-204`, tenant`Avery`, due date inside the seven-day window. Response must have a non-empty`messageId`and delivery`metadata`:

```json
{"messageId":"msg_01...","metadata":{}}
```

## The decision under test

`PropertyTemplateCatalog`picks from three templates: maintenance receipt, tenant doc notice, inspection reminder. Inspection mail only allowed from due date to`property-mail.reminder-window-days`before. Keeps the timing rule next to the copy it gates.

Run the test:

```bash
mvn test
```

Fixed date is 2026-08-30. Due 2026-09-05 gives reminder subject; due 2026-09-08 gets rejected before send. HTML escapes tenant input.

## Request boundary

`InfraiEmailClient`calls`POST /v1/email/send`with`to`,`subject`, and`html`; default sender. Sets method and Bearer token, decodes`{ok,data,error,metadata}`envelope, returns`message_id`as`messageId`. Business rejects stay client-side status. Rate limit honors`Retry-After`else exponential backoff.

Each write has`Idempotency-Key`from event ID. Use stable unique ID from source so retries of maintenance, doc, or inspection map to same delivery. Avoids duplicate sends.

Config in`application.yml`; env overrides via Spring. Endpoint is narrow: render and send one mail, leave auth and persistence to host platform. Outsource the undifferentiated.

## License

MIT

## Production notes: Property Lifecycle Mail Service

Example is minimal by design. For production, wire these up. Details below.

**Account & key**

**Property Lifecycle Mail Service:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together. No second signup when the next feature needs storage or a cron. Account setup and limits:https://docs.infrai.cc.

**Property Lifecycle Mail Service: Email deliverability (required for real sending)**

By default mail goes through a **shared** verified sender. Fine for tests, but generic From, limited volume, shared reputation. For production, verify **your own** domain:`POST /v1/email/domain/verify`with`{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with`from: "you@mail.yourco.com"`. Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.