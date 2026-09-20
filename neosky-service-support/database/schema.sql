-- =====================================================================
-- NeoSky Service & Support — PostgreSQL Schema
-- =====================================================================
-- Design notes:
--  * All primary keys are UUIDs (gen_random_uuid(), pgcrypto extension).
--  * Every customer-owned row carries a direct or indirect customer_id
--    so row-level authorization ("customer isolation") can always be
--    enforced with a single predicate, never by trusting a client-sent id.
--  * updated_at columns are maintained by the set_updated_at() trigger.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------
-- Enums
-- ---------------------------------------------------------------------
CREATE TYPE user_role AS ENUM ('customer', 'service_engineer', 'admin');

CREATE TYPE drone_status AS ENUM ('active', 'under_service', 'grounded', 'retired', 'lost_stolen');

CREATE TYPE component_type AS ENUM (
  'battery', 'controller_gcs', 'payload', 'propeller',
  'motor', 'esc', 'flight_controller', 'other'
);

CREATE TYPE ticket_category AS ENUM (
  'flight_issue', 'hardware_issue', 'software_issue', 'battery',
  'camera_payload', 'controller_gcs', 'connectivity', 'firmware',
  'crash_damage', 'warranty', 'amc_service', 'training', 'other'
);

CREATE TYPE ticket_priority AS ENUM ('low', 'medium', 'high', 'critical');

CREATE TYPE ticket_status AS ENUM (
  'new', 'assigned', 'in_progress', 'waiting_customer',
  'waiting_parts', 'service', 'qc', 'resolved', 'closed'
);

CREATE TYPE attachment_type AS ENUM ('photo', 'video', 'document', 'log');

CREATE TYPE flight_result AS ENUM ('successful', 'aborted', 'crashed', 'partial');

CREATE TYPE sync_status AS ENUM ('synced', 'pending_sync');

CREATE TYPE maintenance_status AS ENUM ('upcoming', 'due_soon', 'due', 'overdue', 'completed');

CREATE TYPE warranty_status AS ENUM ('active', 'expiring_soon', 'expired');

CREATE TYPE warranty_claim_status AS ENUM ('submitted', 'approved', 'rejected', 'completed');

CREATE TYPE payment_status AS ENUM ('paid', 'pending', 'partially_paid', 'cancelled');

CREATE TYPE warranty_type AS ENUM ('warranty', 'non_warranty');

CREATE TYPE document_type AS ENUM (
  'invoice', 'warranty_certificate', 'service_report', 'qc_report',
  'rca_report', 'maintenance_report', 'drone_manual',
  'training_certificate', 'other'
);

CREATE TYPE notification_type AS ENUM (
  'ticket_created', 'ticket_assigned', 'ticket_status_changed', 'engineer_comment',
  'customer_response_required', 'ticket_resolved', 'ticket_closed',
  'warranty_expiring', 'maintenance_due', 'maintenance_overdue',
  'invoice_generated', 'payment_pending', 'service_completed'
);

-- ---------------------------------------------------------------------
-- users  (auth identity — one row per login credential, any role)
-- ---------------------------------------------------------------------
CREATE TABLE users (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email             TEXT UNIQUE NOT NULL,
  phone             TEXT UNIQUE,
  password_hash     TEXT NOT NULL,
  role              user_role NOT NULL DEFAULT 'customer',
  full_name         TEXT NOT NULL,
  is_active         BOOLEAN NOT NULL DEFAULT true,
  is_verified       BOOLEAN NOT NULL DEFAULT false,
  fcm_token         TEXT,
  last_login_at     TIMESTAMPTZ,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- customers  (1:1 profile extension of a 'customer'-role user)
-- ---------------------------------------------------------------------
CREATE TABLE customers (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id           UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  customer_code     TEXT UNIQUE NOT NULL,
  company_name      TEXT,
  billing_address   TEXT,
  gstin             TEXT,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_customers_updated_at BEFORE UPDATE ON customers
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- drones
-- ---------------------------------------------------------------------
CREATE TABLE drones (
  id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id               UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
  drone_name                TEXT NOT NULL,
  model                     TEXT NOT NULL,
  serial_number             TEXT UNIQUE NOT NULL,
  uin                       TEXT,
  purchase_date             DATE,
  delivery_date             DATE,
  invoice_number            TEXT,
  invoice_date              DATE,
  warranty_start_date       DATE,
  warranty_end_date         DATE,
  total_flight_hours        NUMERIC(10,2) NOT NULL DEFAULT 0,
  total_flights             INTEGER NOT NULL DEFAULT 0,
  last_flight_at            TIMESTAMPTZ,
  last_service_at           TIMESTAMPTZ,
  next_maintenance_due_hours NUMERIC(10,2),
  next_maintenance_due_date DATE,
  firmware_version          TEXT,
  status                    drone_status NOT NULL DEFAULT 'active',
  created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_drones_customer_id ON drones(customer_id);
CREATE INDEX idx_drones_serial_number ON drones(serial_number);
CREATE TRIGGER trg_drones_updated_at BEFORE UPDATE ON drones
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- drone_components  (battery / controller-GCS / payload / propeller / ...)
-- ---------------------------------------------------------------------
CREATE TABLE drone_components (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  drone_id        UUID NOT NULL REFERENCES drones(id) ON DELETE CASCADE,
  component_type  component_type NOT NULL,
  name            TEXT NOT NULL,
  serial_number   TEXT,
  cycle_count     INTEGER DEFAULT 0,
  installed_date  DATE,
  status          TEXT NOT NULL DEFAULT 'active',
  metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_drone_components_drone_id ON drone_components(drone_id);
CREATE TRIGGER trg_drone_components_updated_at BEFORE UPDATE ON drone_components
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- tickets
-- ---------------------------------------------------------------------
CREATE TABLE tickets (
  id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ticket_number               TEXT UNIQUE NOT NULL,
  customer_id                 UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
  drone_id                    UUID NOT NULL REFERENCES drones(id) ON DELETE CASCADE,
  category                    ticket_category NOT NULL,
  sub_category                TEXT,
  priority                    ticket_priority NOT NULL DEFAULT 'medium',
  subject                     TEXT NOT NULL,
  description                 TEXT NOT NULL,
  status                      ticket_status NOT NULL DEFAULT 'new',
  issue_datetime              TIMESTAMPTZ,
  location                    TEXT,
  flight_hours_at_issue       NUMERIC(10,2),
  assigned_engineer_id        UUID REFERENCES users(id),
  resolved_at                 TIMESTAMPTZ,
  closed_at                   TIMESTAMPTZ,
  customer_confirmed_resolution BOOLEAN NOT NULL DEFAULT false,
  created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_tickets_customer_id ON tickets(customer_id);
CREATE INDEX idx_tickets_drone_id ON tickets(drone_id);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE TRIGGER trg_tickets_updated_at BEFORE UPDATE ON tickets
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ticket_comments (also serves as the audit trail for the status timeline)
CREATE TABLE ticket_comments (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ticket_id     UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
  author_id     UUID NOT NULL REFERENCES users(id),
  comment       TEXT NOT NULL,
  status_from   ticket_status,
  status_to     ticket_status,
  is_internal   BOOLEAN NOT NULL DEFAULT false,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ticket_comments_ticket_id ON ticket_comments(ticket_id);

CREATE TABLE ticket_attachments (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ticket_id     UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
  uploaded_by   UUID NOT NULL REFERENCES users(id),
  file_url      TEXT NOT NULL,
  file_type     attachment_type NOT NULL,
  file_name     TEXT NOT NULL,
  file_size_bytes BIGINT,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ticket_attachments_ticket_id ON ticket_attachments(ticket_id);

-- ---------------------------------------------------------------------
-- flight_logs
-- ---------------------------------------------------------------------
CREATE TABLE flight_logs (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  client_uuid           UUID UNIQUE,
  drone_id              UUID NOT NULL REFERENCES drones(id) ON DELETE CASCADE,
  customer_id           UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
  pilot_id              UUID NOT NULL REFERENCES users(id),
  flight_date           DATE NOT NULL,
  start_time            TIMESTAMPTZ NOT NULL,
  end_time              TIMESTAMPTZ NOT NULL,
  duration_minutes      NUMERIC(10,2) NOT NULL,
  location              TEXT,
  max_altitude_m        NUMERIC(10,2),
  distance_travelled_km NUMERIC(10,2),
  mission_type          TEXT,
  payload_used          TEXT,
  battery_used          TEXT,
  battery_cycle         INTEGER,
  weather               TEXT,
  flight_result         flight_result NOT NULL DEFAULT 'successful',
  remarks               TEXT,
  incident_flag         BOOLEAN NOT NULL DEFAULT false,
  sync_status           sync_status NOT NULL DEFAULT 'synced',
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_flight_logs_drone_id ON flight_logs(drone_id);
CREATE INDEX idx_flight_logs_customer_id ON flight_logs(customer_id);
CREATE INDEX idx_flight_logs_pilot_id ON flight_logs(pilot_id);
CREATE TRIGGER trg_flight_logs_updated_at BEFORE UPDATE ON flight_logs
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- maintenance_schedule / maintenance_records
-- ---------------------------------------------------------------------
CREATE TABLE maintenance_schedule (
  id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  drone_id                UUID NOT NULL UNIQUE REFERENCES drones(id) ON DELETE CASCADE,
  interval_flight_hours   NUMERIC(10,2),
  interval_calendar_days  INTEGER,
  last_maintenance_date   DATE,
  last_maintenance_hours  NUMERIC(10,2),
  next_due_date           DATE,
  next_due_hours          NUMERIC(10,2),
  status                  maintenance_status NOT NULL DEFAULT 'upcoming',
  created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_maintenance_schedule_updated_at BEFORE UPDATE ON maintenance_schedule
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE maintenance_records (
  id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  drone_id                UUID NOT NULL REFERENCES drones(id) ON DELETE CASCADE,
  maintenance_schedule_id UUID REFERENCES maintenance_schedule(id),
  ticket_id               UUID REFERENCES tickets(id),
  performed_by            UUID REFERENCES users(id),
  maintenance_type        TEXT NOT NULL,
  flight_hours_at_service NUMERIC(10,2),
  description             TEXT,
  parts_replaced          JSONB NOT NULL DEFAULT '[]'::jsonb,
  performed_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  status                  TEXT NOT NULL DEFAULT 'completed',
  created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_maintenance_records_drone_id ON maintenance_records(drone_id);

-- ---------------------------------------------------------------------
-- warranties / warranty_claims
-- ---------------------------------------------------------------------
CREATE TABLE warranties (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  drone_id        UUID NOT NULL UNIQUE REFERENCES drones(id) ON DELETE CASCADE,
  start_date      DATE NOT NULL,
  end_date        DATE NOT NULL,
  status          warranty_status NOT NULL DEFAULT 'active',
  covered_items   TEXT[] NOT NULL DEFAULT '{}',
  excluded_items  TEXT[] NOT NULL DEFAULT '{}',
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_warranties_updated_at BEFORE UPDATE ON warranties
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE warranty_claims (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  warranty_id   UUID NOT NULL REFERENCES warranties(id) ON DELETE CASCADE,
  ticket_id     UUID NOT NULL REFERENCES tickets(id),
  claim_number  TEXT UNIQUE NOT NULL,
  description   TEXT,
  status        warranty_claim_status NOT NULL DEFAULT 'submitted',
  claimed_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  resolved_at   TIMESTAMPTZ,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_warranty_claims_warranty_id ON warranty_claims(warranty_id);

-- ---------------------------------------------------------------------
-- invoices / invoice_items / payments
-- ---------------------------------------------------------------------
CREATE TABLE invoices (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  invoice_number    TEXT UNIQUE NOT NULL,
  customer_id       UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
  drone_id          UUID REFERENCES drones(id),
  invoice_date      DATE NOT NULL,
  product_service   TEXT NOT NULL,
  subtotal_amount   NUMERIC(12,2) NOT NULL DEFAULT 0,
  gst_amount        NUMERIC(12,2) NOT NULL DEFAULT 0,
  total_amount      NUMERIC(12,2) NOT NULL DEFAULT 0,
  payment_status    payment_status NOT NULL DEFAULT 'pending',
  warranty_type     warranty_type NOT NULL DEFAULT 'non_warranty',
  pdf_url           TEXT,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_invoices_customer_id ON invoices(customer_id);
CREATE TRIGGER trg_invoices_updated_at BEFORE UPDATE ON invoices
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE invoice_items (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  invoice_id    UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
  description   TEXT NOT NULL,
  quantity      NUMERIC(10,2) NOT NULL DEFAULT 1,
  unit_price    NUMERIC(12,2) NOT NULL,
  amount        NUMERIC(12,2) NOT NULL
);
CREATE INDEX idx_invoice_items_invoice_id ON invoice_items(invoice_id);

CREATE TABLE payments (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  invoice_id        UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
  amount            NUMERIC(12,2) NOT NULL,
  payment_date      TIMESTAMPTZ NOT NULL DEFAULT now(),
  payment_method    TEXT,
  reference_number  TEXT,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_payments_invoice_id ON payments(invoice_id);

-- ---------------------------------------------------------------------
-- service_records  (chronological, human-readable service history)
-- ---------------------------------------------------------------------
CREATE TABLE service_records (
  id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  drone_id                UUID NOT NULL REFERENCES drones(id) ON DELETE CASCADE,
  ticket_id               UUID REFERENCES tickets(id),
  maintenance_record_id   UUID REFERENCES maintenance_records(id),
  service_date            DATE NOT NULL,
  issue_summary           TEXT,
  action_taken            TEXT,
  performed_by            UUID REFERENCES users(id),
  status                  TEXT NOT NULL DEFAULT 'completed',
  created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_service_records_drone_id ON service_records(drone_id);

-- ---------------------------------------------------------------------
-- documents
-- ---------------------------------------------------------------------
CREATE TABLE documents (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  document_type         document_type NOT NULL,
  customer_id           UUID REFERENCES customers(id) ON DELETE CASCADE,
  drone_id              UUID REFERENCES drones(id) ON DELETE CASCADE,
  ticket_id             UUID REFERENCES tickets(id) ON DELETE CASCADE,
  service_record_id     UUID REFERENCES service_records(id) ON DELETE CASCADE,
  file_name             TEXT NOT NULL,
  file_url              TEXT NOT NULL,
  uploaded_by           UUID REFERENCES users(id),
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT documents_linked_to_something CHECK (
    customer_id IS NOT NULL OR drone_id IS NOT NULL OR
    ticket_id IS NOT NULL OR service_record_id IS NOT NULL
  )
);
CREATE INDEX idx_documents_customer_id ON documents(customer_id);
CREATE INDEX idx_documents_drone_id ON documents(drone_id);
CREATE INDEX idx_documents_ticket_id ON documents(ticket_id);

-- ---------------------------------------------------------------------
-- notifications
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type                notification_type NOT NULL,
  title               TEXT NOT NULL,
  body                TEXT NOT NULL,
  related_entity_type TEXT,
  related_entity_id   UUID,
  is_read             BOOLEAN NOT NULL DEFAULT false,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user_id ON notifications(user_id, is_read);

-- ---------------------------------------------------------------------
-- audit_logs
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       UUID REFERENCES users(id),
  action        TEXT NOT NULL,
  entity_type   TEXT NOT NULL,
  entity_id     UUID,
  ip_address    TEXT,
  metadata      JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
