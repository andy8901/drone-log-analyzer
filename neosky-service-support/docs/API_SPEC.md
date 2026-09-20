# NeoSky Service & Support — REST API Specification

Base URL: `https://api.neosky.example.com/api` (local dev: `http://localhost:8000/api`)

All authenticated requests send `Authorization: Bearer <access_token>`.
All request/response bodies are JSON. Dates are ISO-8601 (`YYYY-MM-DD`), timestamps are ISO-8601 UTC (`YYYY-MM-DDTHH:MM:SSZ`).

## Conventions

- Pagination: list endpoints accept `?page=1&page_size=20` and return:
  ```json
  { "items": [...], "page": 1, "page_size": 20, "total": 42, "total_pages": 3 }
  ```
- Errors follow a single shape:
  ```json
  { "error": { "code": "VALIDATION_ERROR", "message": "email already registered", "fields": { "email": "already registered" } } }
  ```
- **Customer data isolation**: every customer-scoped endpoint derives `customer_id` from the authenticated JWT, never from a path/query/body parameter. Requesting `/api/drones/{id}` for a drone owned by another customer returns `404 Not Found` (not `403`, to avoid confirming the id exists).
- Roles: `customer`, `service_engineer`, `admin`. Engineer/admin-only endpoints are under `/api/admin/*` and `/api/engineer/*`.

---

## 1. Authentication

### POST /api/auth/register
Request:
```json
{ "full_name": "Aniket Rao", "email": "aniket@example.com", "phone": "+919800000000", "password": "Str0ngPass!", "company_name": "Throttle Aero" }
```
Response `201`:
```json
{ "user": { "id": "uuid", "email": "...", "full_name": "...", "role": "customer" }, "message": "Verification OTP sent" }
```

### POST /api/auth/verify-otp
`{ "email": "...", "otp": "123456" }` → `200 { "verified": true }`

### POST /api/auth/login
`{ "identifier": "email-or-phone", "password": "..." }` → `200`
```json
{ "access_token": "...", "refresh_token": "...", "token_type": "bearer", "expires_in": 900,
  "user": { "id": "uuid", "full_name": "...", "email": "...", "role": "customer" } }
```

### POST /api/auth/refresh
`{ "refresh_token": "..." }` → `200 { "access_token": "...", "expires_in": 900 }`

### POST /api/auth/forgot-password
`{ "identifier": "email-or-phone" }` → `200 { "message": "OTP sent" }`

### POST /api/auth/reset-password
`{ "identifier": "...", "otp": "123456", "new_password": "..." }` → `200 { "message": "Password updated" }`

### POST /api/auth/logout
Invalidates the refresh token. `204`

---

## 2. Customer Profile

### GET /api/customer/profile
`200`:
```json
{ "id": "uuid", "customer_code": "NS-CUST-000123", "full_name": "...", "email": "...", "phone": "...",
  "company_name": "...", "billing_address": "...", "gstin": "..." }
```

### PUT /api/customer/profile
Body: any subset of `{ full_name, phone, company_name, billing_address, gstin, fcm_token }` → `200` updated profile.

---

## 3. Dashboard

### GET /api/dashboard
`200`:
```json
{
  "registered_drones": 4, "active_warranty_drones": 3, "warranty_expiring_soon": 1,
  "open_tickets": 2, "pending_service_requests": 1, "upcoming_maintenance": 1,
  "total_flight_hours": 126.5,
  "recent_flight": { "id": "uuid", "drone_name": "TAVAS-0012", "flight_date": "2026-09-18", "duration_minutes": 32.0 },
  "recent_ticket": { "id": "uuid", "ticket_number": "NS-2026-00125", "subject": "Video feed loss", "status": "in_progress" },
  "recent_invoice": { "id": "uuid", "invoice_number": "INV-2026-00098", "total_amount": 4500.0, "payment_status": "pending" }
}
```

---

## 4. Drones

### GET /api/drones
List drones for the authenticated customer only. Supports `?status=active`.

### GET /api/drones/{id}
Full drone profile including components (battery / controller-GCS / payload), latest warranty summary, and maintenance summary.

### POST /api/drones
Admin/engineer registers a drone against a customer (see §17). Not available to `customer` role.

### GET /api/drones/{id}/components
### GET /api/drones/search?q=TAVAS-0012

---

## 5. Tickets

### GET /api/tickets
`?status=open|closed&drone_id=uuid&page=1&page_size=20`

### GET /api/tickets/{id}
Includes `timeline` (derived from `ticket_comments` status transitions), `attachments`, `comments`.

### POST /api/tickets
```json
{
  "drone_id": "uuid", "category": "software_issue", "sub_category": "Video feed",
  "priority": "high", "subject": "Video feed loss", "description": "...",
  "issue_datetime": "2026-09-20T10:30:00Z", "location": "Pune", "flight_hours_at_issue": 43.5
}
```
`201` → created ticket with `ticket_number` auto-generated as `NS-{year}-{seq5}`.

### POST /api/tickets/{id}/comments
`{ "comment": "..." }` → `201`

### POST /api/tickets/{id}/attachments
`multipart/form-data`: `file`, `file_type` (`photo|video|document|log`) → `201` attachment record.

### POST /api/tickets/{id}/close
Customer-only, only when `status == resolved`. `{ "confirmed": true, "feedback": "..." }` → `200`, sets `status=closed`, `customer_confirmed_resolution=true`.

---

## 6. Flight Logs

### GET /api/flights
`?drone_id=uuid&from=2026-01-01&to=2026-12-31&page=1&page_size=20`

### GET /api/flights/stats
```json
{ "total_flights": 58, "total_flight_hours": 126.5, "monthly_flight_hours": [{"month":"2026-08","hours":12.4}, ...],
  "average_duration_minutes": 28.4, "hours_per_drone": [{"drone_id":"uuid","drone_name":"TAVAS-0012","hours":60.2}],
  "hours_per_pilot": [{"pilot_id":"uuid","pilot_name":"...","hours":80.1}] }
```

### POST /api/flights
```json
{
  "client_uuid": "local-generated-uuid", "drone_id": "uuid",
  "start_time": "2026-09-20T09:00:00Z", "end_time": "2026-09-20T09:32:00Z",
  "duration_minutes": null, "location": "Pune", "max_altitude_m": 120,
  "distance_travelled_km": 3.2, "mission_type": "Survey", "payload_used": "RGB Camera",
  "battery_used": "BATT-004", "battery_cycle": 112, "weather": "Clear",
  "flight_result": "successful", "remarks": "", "incident_flag": false
}
```
`duration_minutes: null` → server computes `end_time - start_time`. `client_uuid` makes retries idempotent (offline sync).

### PUT /api/flights/{id}
### DELETE /api/flights/{id}

---

## 7. Warranty

### GET /api/drones/{id}/warranty
```json
{ "drone_id": "uuid", "start_date": "2025-01-31", "end_date": "2027-01-31", "days_remaining": 133,
  "status": "active", "covered_items": ["Airframe","Flight Controller"], "excluded_items": ["Propellers","Crash damage"],
  "claims": [ { "id": "uuid", "claim_number": "WC-2026-0007", "ticket_id": "uuid", "status": "completed" } ] }
```

---

## 8. Maintenance

### GET /api/drones/{id}/maintenance
```json
{ "schedule": { "interval_flight_hours": 50, "interval_calendar_days": 90, "last_maintenance_date": "2026-08-01",
    "current_flight_hours": 43.5, "next_due_hours": 50, "remaining_hours": 6.5, "next_due_date": "2026-11-01",
    "status": "due_soon" },
  "records": [ { "id":"uuid","maintenance_type":"scheduled","performed_at":"2026-08-01","description":"Preventive maintenance","status":"completed" } ] }
```

### POST /api/drones/{id}/maintenance
Engineer/admin logs a maintenance event or updates the schedule (see §17).

---

## 9. Invoices

### GET /api/invoices `?status=pending&page=1`
### GET /api/invoices/{id} (with `invoice_items`, `payments`)
### GET /api/invoices/{id}/pdf → binary `application/pdf`

---

## 10. Service History

### GET /api/drones/{id}/service-history
Chronological (`service_date desc`) list combining `service_records`.

---

## 11. Documents

### GET /api/documents `?drone_id=uuid&ticket_id=uuid&type=warranty_certificate`
### GET /api/documents/{id}/download

---

## 12. Notifications

### GET /api/notifications `?unread_only=true&page=1`
### POST /api/notifications/{id}/read
### POST /api/notifications/read-all
### PUT /api/customer/fcm-token — `{ "fcm_token": "..." }` registers device for push.

---

## 13. Search

### GET /api/search?q=TAVAS-0012
```json
{ "drones": [...], "tickets": [...], "invoices": [...], "service_records": [...] }
```
Scoped to the authenticated customer's own records.

---

## 14. Admin / Engineer Portal (`role in {service_engineer, admin}`)

- `GET  /api/admin/customers` / `GET /api/admin/customers/{id}`
- `GET  /api/admin/drones` (all customers) / `POST /api/admin/drones` (register drone)
- `GET  /api/admin/tickets` / `PUT /api/admin/tickets/{id}/assign` `{ "engineer_id": "uuid" }`
- `PUT  /api/admin/tickets/{id}/status` `{ "status": "in_progress", "comment": "..." }` (drives the timeline + push notification)
- `POST /api/admin/tickets/{id}/comments` (internal or customer-visible)
- `POST /api/admin/service-records` — record a completed service, optionally attaching a report document
- `PUT  /api/admin/drones/{id}/warranty` — update warranty coverage
- `POST /api/admin/drones/{id}/warranty/claims`
- `POST /api/admin/invoices` — generate an invoice (+ `invoice_items`)
- `POST /api/admin/drones/{id}/maintenance/schedule`
- `GET  /api/admin/drones/{id}/flights` — full flight history for any drone
- `GET  /api/admin/customers/{id}/history` — full 360° customer history

This surface is designed to back a future web Service Dashboard without any schema changes.

---

## Status Enumerations (mirrors DB enums in `database/schema.sql`)

- `drone_status`: active, under_service, grounded, retired, lost_stolen
- `ticket_status`: new, assigned, in_progress, waiting_customer, waiting_parts, service, qc, resolved, closed
- `ticket_priority`: low, medium, high, critical
- `ticket_category`: flight_issue, hardware_issue, software_issue, battery, camera_payload, controller_gcs, connectivity, firmware, crash_damage, warranty, amc_service, training, other
- `maintenance_status`: upcoming, due_soon, due, overdue, completed
- `warranty_status`: active, expiring_soon, expired
- `payment_status`: paid, pending, partially_paid, cancelled
- `notification_type`: ticket_created, ticket_assigned, ticket_status_changed, engineer_comment, customer_response_required, ticket_resolved, ticket_closed, warranty_expiring, maintenance_due, maintenance_overdue, invoice_generated, payment_pending, service_completed

## Auth & Security Requirements (binding on the implementation)

1. JWT access tokens (short-lived, 15 min) + rotating refresh tokens (7–30 days), stored hashed server-side.
2. Passwords hashed with bcrypt (never reversible, never logged).
3. Every customer-scoped query filters by `customer_id` derived from the token — path/body ids are only used to select *within* that customer's own rows.
4. Role-based dependency guards on every `/api/admin/*` and `/api/engineer/*` route.
5. File uploads validated by content-type + extension allowlist and size limit; stored outside the web root / in object storage, served via signed URLs.
6. All mutating admin actions written to `audit_logs`.
7. HTTPS-only in production (HSTS); CORS locked to known origins.
