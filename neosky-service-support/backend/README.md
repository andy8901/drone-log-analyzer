# NeoSky Service & Support — Backend

FastAPI + SQLAlchemy 2.0 + PostgreSQL backend for the NeoSky Service & Support
drone customer-service app. Implements the contract in
[`../docs/API_SPEC.md`](../docs/API_SPEC.md) against the schema in
[`../database/schema.sql`](../database/schema.sql) — schema.sql is the single
source of truth for the database shape; the SQLAlchemy models in `app/models/`
only ever *map onto* tables it creates, they never create or alter schema.

## Quick start — Docker Compose

```bash
cd neosky-service-support/backend
docker compose up --build
```

This starts Postgres 16 and the API. On first boot the entrypoint waits for
Postgres, then runs `scripts/init_db.py` (applies `../database/schema.sql`)
and `scripts/seed.py` (loads sample data) — both are idempotent, so restarts
are safe. The API is then up at `http://localhost:8000`.

## Quick start — manual / local

```bash
cd neosky-service-support/backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

cp .env.example .env
# edit .env: set DATABASE_URL to a Postgres instance you control

python scripts/init_db.py   # applies ../database/schema.sql (run once against an empty DB)
python scripts/seed.py      # loads sample data (idempotent; safe to re-run)

uvicorn app.main:app --reload
```

The API is then up at `http://localhost:8000`.

## Interactive API docs

FastAPI auto-serves OpenAPI/Swagger UI at **`/docs`** (and ReDoc at `/redoc`)
once the app is running. This doubles as the live, always-in-sync REST API
specification alongside the static [`../docs/API_SPEC.md`](../docs/API_SPEC.md).

## Seeded login credentials

Password for every seeded account is **`NeoSky@123`**.

| Role               | Email                        | Notes                                    |
|--------------------|-------------------------------|-------------------------------------------|
| customer           | `aniket@throttle.aero`        | Throttle Aero — 3 drones, tickets, invoices, flights, etc. |
| customer           | `owner@skyfleet.example`      | SkyFleet Logistics — 1 drone (for isolation testing) |
| service_engineer   | `engineer@neosky.example`     | assigned to the in-progress ticket        |
| admin              | `admin@neosky.example`        |                                            |

## Running tests

```bash
source .venv/bin/activate
pip install -r requirements.txt   # includes pytest

# Pure unit tests (no DB required):
pytest tests/test_security_units.py tests/test_flight_duration.py -v

# Full suite, including DB-backed integration tests:
export TEST_DATABASE_URL=postgresql://postgres:postgres@localhost:5432/neosky_test
pytest -v
```

DB-dependent tests (`test_auth_flow.py`, `test_customer_isolation.py`,
`test_ticket_lifecycle.py`) are skipped cleanly via `pytest.mark.skipif` if
`TEST_DATABASE_URL` is not set. When it is set, each test run drops and
recreates the `public` schema in that database and re-applies
`../database/schema.sql` fresh, then truncates all tables before each test —
point it at a disposable/throwaway database, never a database you care about.

## Project layout

```
app/
  main.py            FastAPI app, CORS, router wiring, global exception handler
  config.py          pydantic-settings Settings (env-driven)
  database.py        SQLAlchemy engine/session, get_db dependency
  models/            SQLAlchemy ORM models mirroring database/schema.sql exactly
  schemas/           Pydantic request/response models matching API_SPEC.md
  core/
    security.py      password hashing, JWT issuance/verification, get_current_user, require_role
    customer_scope.py  get_current_customer — the only place customer_id is derived from the JWT
    ownership.py     get_owned_*_or_404 helpers — the only way routers look up a customer's own rows
    audit.py         write_audit_log — every mutating admin action calls this
    otp.py           ephemeral in-process OTP store (see note below)
  services/          business logic (ticket numbering, flight duration/upsert,
                     warranty/maintenance status derivation, notifications, invoices, uploads)
  routers/           one module per API_SPEC.md section (auth, customer, dashboard,
                     drones, tickets, flights, warranty, maintenance, invoices,
                     service_history, documents, notifications, search, admin)
scripts/
  init_db.py         applies ../database/schema.sql verbatim (idempotent no-op if already applied)
  seed.py            idempotent sample-data seeder
tests/               pytest suite (see above)
```

## Security notes

- Every customer-facing endpoint that takes a resource id resolves
  `customer_id` **only** from the authenticated JWT (`get_current_customer`),
  never from a path/query/body parameter, and looks up the resource through a
  `get_owned_*_or_404` helper (`app/core/ownership.py`) that filters by that
  `customer_id`. A row that exists but belongs to another customer returns
  `404`, not `403` — see `tests/test_customer_isolation.py`.
- Passwords are hashed with bcrypt via `passlib`, never logged or returned.
- `/api/admin/*` routes are gated with `require_role("service_engineer", "admin")`.
- Every mutating admin action writes an `audit_logs` row via `write_audit_log`.
- Uploaded files are validated by extension + content-type allowlist and a
  size limit, then stored under `UPLOAD_DIR` (outside any served static
  root); ticket-attachment and document downloads are only ever served after
  an ownership check.

## Deliberate deviations from the spec (schema.sql is fixed and could not be changed)

`database/schema.sql` has exactly 19 tables and is the binding, unmodifiable
contract for this build. Two small pieces of the spec assume server-side
storage that schema.sql does not provide a table for:

1. **Refresh tokens.** The spec's "Auth & Security Requirements" call for
   rotating refresh tokens "stored hashed server-side," but schema.sql has no
   refresh-token table. Refresh tokens are implemented as self-contained,
   signed JWTs (`type: "refresh"`, their own longer expiry), verified the
   same way as access tokens (`app/core/security.py`). `POST /api/auth/logout`
   therefore returns `204` without a server-side revocation step — there is
   nowhere to persist revocation state without altering schema.sql.
2. **OTP codes** (`register` → `verify-otp`, `forgot-password` →
   `reset-password`) have no backing table either, and no SMS/email gateway
   is wired up (none was specified). OTPs are kept in an in-process store
   (`app/core/otp.py`) and logged server-side instead of being sent over a
   real channel — adequate for a stub verification flow, but ephemeral
   (does not survive a restart, not shared across worker processes).

No other deviations from `API_SPEC.md` or `database/schema.sql` were made.
