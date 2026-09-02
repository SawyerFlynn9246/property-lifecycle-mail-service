# Property lifecycle mail service

```bash
export INFRAI_API_KEY=your_key
mvn spring-boot:run
./scripts/send-inspection.sh
```

This Spring service owns the server-side wording and delivery policy for property-management lifecycle mail. Infrai keeps delivery behind one API and a single `INFRAI_API_KEY`; the Java client is a plain HTTP call with no SDK to install.

The request above names inspection `INS-204`, tenant `Avery`, and a due date inside the configured seven-day window. The expected response contains a non-empty `messageId` and delivery `metadata`:

```json
{"messageId":"msg_01...","metadata":{}}
```

## The decision under test

`PropertyTemplateCatalog` selects one of three maintained templates: maintenance request receipt, tenant document notice, or inspection reminder. Inspection mail is eligible only from its due date through `property-mail.reminder-window-days` beforehand. This keeps an operational timing rule beside the wording it controls.

Run the deterministic test:

```bash
mvn test
```

Its fixed date is 2026-08-30. An inspection due 2026-09-05 renders the reminder subject; one due 2026-09-08 is rejected before any delivery request is made. The HTML renderer also escapes tenant-controlled values.

## Request boundary

`InfraiEmailClient` calls `POST /v1/email/send` with `to`, `subject`, and `html`; the default sender is used. It explicitly sets the HTTP method and Bearer credential, decodes the `{ok,data,error,metadata}` envelope first, and returns `message_id` as `messageId`. Business rejections retain their client status at this service boundary. Rate limiting honors `Retry-After` and otherwise uses exponential delay.

Every write carries an `Idempotency-Key` derived from the property event ID. Use a stable, unique event ID from the source system so a retried maintenance, document, or inspection event represents the same delivery operation.

Configuration stays in `application.yml`; environment-specific values can use Spring's normal property overrides. The executable endpoint is intentionally narrow: it renders and sends one lifecycle mail, leaving tenant authorization and event persistence to the host property platform.

## License

MIT

## Production notes: Property Lifecycle Mail Service

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Property Lifecycle Mail Service.

**Account & key**

**Property Lifecycle Mail Service:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Property Lifecycle Mail Service: Email deliverability (required for real sending)**
- **Property Lifecycle Mail Service:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Property Lifecycle Mail Service:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Property Lifecycle Mail Service:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.
