#!/usr/bin/env python3
"""Idempotent sample-data seed script for NeoSky Service & Support.

Run after scripts/init_db.py:
    python scripts/seed.py
"""

import os
import sys
import uuid
from datetime import date, datetime, timedelta, timezone

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from dotenv import load_dotenv  # noqa: E402

load_dotenv()

from app.core.security import hash_password  # noqa: E402
from app.database import SessionLocal  # noqa: E402
from app.models.customer import Customer  # noqa: E402
from app.models.document import Document  # noqa: E402
from app.models.drone import Drone, DroneComponent  # noqa: E402
from app.models.flight import FlightLog  # noqa: E402
from app.models.invoice import Invoice, InvoiceItem, Payment  # noqa: E402
from app.models.maintenance import MaintenanceRecord, MaintenanceSchedule  # noqa: E402
from app.models.notification import Notification  # noqa: E402
from app.models.service_record import ServiceRecord  # noqa: E402
from app.models.ticket import Ticket, TicketComment  # noqa: E402
from app.models.user import User  # noqa: E402
from app.models.warranty import Warranty  # noqa: E402

PASSWORD = "NeoSky@123"


def get_or_none(db, model, **filters):
    return db.query(model).filter_by(**filters).first()


def make_user(db, *, email, full_name, role, phone=None):
    existing = get_or_none(db, User, email=email)
    if existing:
        return existing, False
    user = User(
        email=email,
        phone=phone,
        password_hash=hash_password(PASSWORD),
        role=role,
        full_name=full_name,
        is_active=True,
        is_verified=True,
    )
    db.add(user)
    db.flush()
    return user, True


def make_drone(db, *, customer, drone_name, model, serial_number, status, purchase_days_ago, warranty_days):
    existing = get_or_none(db, Drone, serial_number=serial_number)
    if existing:
        return existing, False
    today = date.today()
    purchase_date = today - timedelta(days=purchase_days_ago)
    drone = Drone(
        customer_id=customer.id,
        drone_name=drone_name,
        model=model,
        serial_number=serial_number,
        uin=f"UIN-{serial_number}",
        purchase_date=purchase_date,
        delivery_date=purchase_date + timedelta(days=5),
        invoice_number=f"INV-PURCH-{serial_number}",
        invoice_date=purchase_date,
        warranty_start_date=purchase_date,
        warranty_end_date=purchase_date + timedelta(days=warranty_days),
        firmware_version="1.4.2",
        status=status,
    )
    db.add(drone)
    db.flush()
    return drone, True


def make_components(db, drone):
    if db.query(DroneComponent).filter(DroneComponent.drone_id == drone.id).first():
        return
    specs = [
        ("battery", "Li-Po 6S 22000mAh", 40),
        ("controller_gcs", "NeoSky GCS Pro", 0),
        ("payload", "RGB/Thermal Gimbal Camera", 0),
    ]
    for component_type, name, cycles in specs:
        db.add(
            DroneComponent(
                drone_id=drone.id,
                component_type=component_type,
                name=name,
                serial_number=f"{component_type.upper()}-{drone.serial_number}",
                cycle_count=cycles,
                installed_date=drone.purchase_date,
                status="active",
            )
        )


def make_warranty(db, drone):
    if db.query(Warranty).filter(Warranty.drone_id == drone.id).first():
        return
    db.add(
        Warranty(
            drone_id=drone.id,
            start_date=drone.warranty_start_date,
            end_date=drone.warranty_end_date,
            covered_items=["Airframe", "Flight Controller", "Motors", "ESC"],
            excluded_items=["Propellers", "Crash damage", "Water damage"],
        )
    )


def make_maintenance_schedule(db, drone, last_maintenance_hours=20.0, last_maintenance_days_ago=45):
    if db.query(MaintenanceSchedule).filter(MaintenanceSchedule.drone_id == drone.id).first():
        return
    last_date = date.today() - timedelta(days=last_maintenance_days_ago)
    db.add(
        MaintenanceSchedule(
            drone_id=drone.id,
            interval_flight_hours=50,
            interval_calendar_days=90,
            last_maintenance_date=last_date,
            last_maintenance_hours=last_maintenance_hours,
            next_due_date=last_date + timedelta(days=90),
            next_due_hours=last_maintenance_hours + 50,
            status="upcoming",
        )
    )


def make_flights(db, drone, pilot, count, base_hours_offset=0.0):
    if db.query(FlightLog).filter(FlightLog.drone_id == drone.id).first():
        return 0.0, 0
    total_minutes = 0.0
    now = datetime.now(timezone.utc)
    for i in range(count):
        days_ago = int(120 * (i + 1) / count)
        start = now - timedelta(days=days_ago, hours=2)
        duration = 20 + (i % 5) * 6  # 20..44 minutes
        end = start + timedelta(minutes=duration)
        db.add(
            FlightLog(
                client_uuid=uuid.uuid4(),
                drone_id=drone.id,
                customer_id=drone.customer_id,
                pilot_id=pilot.id,
                flight_date=start.date(),
                start_time=start,
                end_time=end,
                duration_minutes=duration,
                location="Pune, MH",
                max_altitude_m=80 + (i % 4) * 10,
                distance_travelled_km=round(1.5 + i * 0.3, 2),
                mission_type="Survey" if i % 2 == 0 else "Inspection",
                payload_used="RGB Camera",
                battery_used=f"BATT-{(i % 3) + 1:03d}",
                battery_cycle=10 + i,
                weather="Clear",
                flight_result="successful" if i % 7 != 0 else "aborted",
                remarks="",
                incident_flag=False,
                sync_status="synced",
            )
        )
        total_minutes += duration
    db.flush()
    drone.total_flight_hours = round(total_minutes / 60.0, 2)
    drone.total_flights = count
    drone.last_flight_at = now - timedelta(days=int(120 / count), hours=2) + timedelta(minutes=duration)
    db.add(drone)
    return total_minutes, count


def next_ticket_number(db):
    year = datetime.now(timezone.utc).year
    prefix = f"NS-{year}-"
    count = db.query(Ticket).filter(Ticket.ticket_number.like(f"{prefix}%")).count()
    return f"{prefix}{count + 1:05d}"


def make_ticket(
    db,
    *,
    customer,
    drone,
    author,
    category,
    subject,
    description,
    priority,
    status_,
    assigned_engineer=None,
    resolved=False,
    closed=False,
):
    existing = get_or_none(db, Ticket, subject=subject, customer_id=customer.id)
    if existing:
        return existing, False

    ticket = Ticket(
        ticket_number=next_ticket_number(db),
        customer_id=customer.id,
        drone_id=drone.id,
        category=category,
        sub_category=None,
        priority=priority,
        subject=subject,
        description=description,
        status=status_,
        issue_datetime=datetime.now(timezone.utc) - timedelta(days=10),
        location="Pune",
        flight_hours_at_issue=float(drone.total_flight_hours),
        assigned_engineer_id=assigned_engineer.id if assigned_engineer else None,
    )
    if resolved or closed:
        ticket.resolved_at = datetime.now(timezone.utc) - timedelta(days=2)
    if closed:
        ticket.closed_at = datetime.now(timezone.utc) - timedelta(days=1)
        ticket.customer_confirmed_resolution = True
    db.add(ticket)
    db.flush()

    db.add(
        TicketComment(
            ticket_id=ticket.id,
            author_id=author.id,
            comment="Ticket created.",
            status_from=None,
            status_to=None,
        )
    )
    if assigned_engineer:
        db.add(
            TicketComment(
                ticket_id=ticket.id,
                author_id=author.id,
                comment=f"Assigned to {assigned_engineer.full_name}.",
                status_from="new",
                status_to="assigned",
            )
        )
    if status_ in ("in_progress", "service", "qc", "resolved", "closed"):
        db.add(
            TicketComment(
                ticket_id=ticket.id,
                author_id=assigned_engineer.id if assigned_engineer else author.id,
                comment="Investigation in progress.",
                status_from="assigned",
                status_to="in_progress",
            )
        )
    if resolved or closed:
        db.add(
            TicketComment(
                ticket_id=ticket.id,
                author_id=assigned_engineer.id if assigned_engineer else author.id,
                comment="Issue resolved after component replacement.",
                status_from="in_progress",
                status_to="resolved",
            )
        )
    if closed:
        db.add(
            TicketComment(
                ticket_id=ticket.id,
                author_id=author.id,
                comment="Customer confirmed resolution and closed the ticket.",
                status_from="resolved",
                status_to="closed",
            )
        )
    return ticket, True


def make_invoice(db, *, customer, drone, product_service, items, payment_status, days_ago=5):
    existing = get_or_none(db, Invoice, product_service=product_service, customer_id=customer.id)
    if existing:
        return existing, False
    subtotal = sum(q * p for _, q, p in items)
    gst = round(subtotal * 0.18, 2)
    total = round(subtotal + gst, 2)
    year = datetime.now(timezone.utc).year
    count = db.query(Invoice).filter(Invoice.invoice_number.like(f"INV-{year}-%")).count()
    invoice = Invoice(
        invoice_number=f"INV-{year}-{count + 1:05d}",
        customer_id=customer.id,
        drone_id=drone.id if drone else None,
        invoice_date=date.today() - timedelta(days=days_ago),
        product_service=product_service,
        subtotal_amount=round(subtotal, 2),
        gst_amount=gst,
        total_amount=total,
        payment_status=payment_status,
        warranty_type="non_warranty",
    )
    db.add(invoice)
    db.flush()
    for desc, qty, unit_price in items:
        db.add(
            InvoiceItem(
                invoice_id=invoice.id,
                description=desc,
                quantity=qty,
                unit_price=unit_price,
                amount=round(qty * unit_price, 2),
            )
        )
    if payment_status == "paid":
        db.add(
            Payment(
                invoice_id=invoice.id,
                amount=total,
                payment_date=datetime.now(timezone.utc) - timedelta(days=days_ago - 1),
                payment_method="UPI",
                reference_number=f"PAY-{invoice.invoice_number}",
            )
        )
    return invoice, True


def main():
    db = SessionLocal()
    try:
        existing_primary = get_or_none(db, User, email="aniket@throttle.aero")
        if existing_primary:
            print("Seed data already present (aniket@throttle.aero exists) — skipping seed.")
            print_credentials()
            return

        # ---- Engineer / Admin users ----
        engineer, _ = make_user(
            db,
            email="engineer@neosky.example",
            full_name="Rahul Desai",
            role="service_engineer",
            phone="+919811111111",
        )
        admin, _ = make_user(
            db,
            email="admin@neosky.example",
            full_name="Priya Sharma",
            role="admin",
            phone="+919822222222",
        )

        # ---- Primary customer: Aniket Rao / Throttle Aero ----
        user1, _ = make_user(
            db,
            email="aniket@throttle.aero",
            full_name="Aniket Rao",
            role="customer",
            phone="+919833333333",
        )
        customer1 = Customer(
            user_id=user1.id,
            customer_code="NS-CUST-000001",
            company_name="Throttle Aero",
            billing_address="123 Aviation Way, Pune, MH 411001",
            gstin="27ABCDE1234F1Z5",
        )
        db.add(customer1)
        db.flush()

        drone11, _ = make_drone(
            db,
            customer=customer1,
            drone_name="TAVAS-0011",
            model="TAVAS Mk3",
            serial_number="SN-TAVAS-0011",
            status="active",
            purchase_days_ago=400,
            warranty_days=730,
        )
        drone12, _ = make_drone(
            db,
            customer=customer1,
            drone_name="TAVAS-0012",
            model="TAVAS Mk3",
            serial_number="SN-TAVAS-0012",
            status="under_service",
            purchase_days_ago=300,
            warranty_days=730,
        )
        drone13, _ = make_drone(
            db,
            customer=customer1,
            drone_name="TAVAS-0013",
            model="TAVAS Mk4",
            serial_number="SN-TAVAS-0013",
            status="active",
            purchase_days_ago=100,
            warranty_days=730,
        )

        for d in (drone11, drone12, drone13):
            make_components(db, d)
            make_warranty(db, d)
        make_maintenance_schedule(db, drone11, last_maintenance_hours=25.0, last_maintenance_days_ago=80)
        make_maintenance_schedule(db, drone12, last_maintenance_hours=45.0, last_maintenance_days_ago=85)
        make_maintenance_schedule(db, drone13, last_maintenance_hours=5.0, last_maintenance_days_ago=20)

        make_flights(db, drone11, user1, count=8)
        make_flights(db, drone12, user1, count=7)
        make_flights(db, drone13, user1, count=4)
        db.flush()

        ticket1, _ = make_ticket(
            db,
            customer=customer1,
            drone=drone12,
            author=user1,
            category="software_issue",
            subject="Video feed loss during flight",
            description="Video feed drops intermittently after ~10 minutes of flight.",
            priority="high",
            status_="in_progress",
            assigned_engineer=engineer,
        )
        ticket2, _ = make_ticket(
            db,
            customer=customer1,
            drone=drone11,
            author=user1,
            category="battery",
            subject="Battery not holding full charge",
            description="Battery 2 discharges to 80% within an hour of being fully charged.",
            priority="medium",
            status_="new",
        )
        ticket3, _ = make_ticket(
            db,
            customer=customer1,
            drone=drone13,
            author=user1,
            category="hardware_issue",
            subject="Landing gear misalignment",
            description="Front-left landing leg appears bent after a hard landing.",
            priority="low",
            status_="closed",
            assigned_engineer=engineer,
            resolved=True,
            closed=True,
        )
        db.flush()

        invoice1, _ = make_invoice(
            db,
            customer=customer1,
            drone=drone12,
            product_service="AMC Service - Q3",
            items=[("Annual Maintenance Contract - Q3", 1, 3500.0), ("Firmware update service", 1, 500.0)],
            payment_status="paid",
            days_ago=20,
        )
        invoice2, _ = make_invoice(
            db,
            customer=customer1,
            drone=drone11,
            product_service="Battery replacement",
            items=[("Li-Po 6S 22000mAh replacement battery", 1, 4200.0), ("Labour", 1, 300.0)],
            payment_status="pending",
            days_ago=3,
        )
        db.flush()

        if not db.query(ServiceRecord).filter(ServiceRecord.drone_id == drone12.id).first():
            mrec = MaintenanceRecord(
                drone_id=drone12.id,
                ticket_id=ticket1.id,
                performed_by=engineer.id,
                maintenance_type="corrective",
                flight_hours_at_service=float(drone12.total_flight_hours),
                description="Replaced video transmitter module.",
                parts_replaced=[{"part": "Video TX Module", "qty": 1}],
                performed_at=datetime.now(timezone.utc) - timedelta(days=15),
            )
            db.add(mrec)
            db.flush()
            db.add(
                ServiceRecord(
                    drone_id=drone12.id,
                    ticket_id=ticket1.id,
                    maintenance_record_id=mrec.id,
                    service_date=date.today() - timedelta(days=15),
                    issue_summary="Video feed loss",
                    action_taken="Replaced video TX module and retested.",
                    performed_by=engineer.id,
                    status="completed",
                )
            )
        if not db.query(ServiceRecord).filter(ServiceRecord.drone_id == drone13.id).first():
            db.add(
                ServiceRecord(
                    drone_id=drone13.id,
                    ticket_id=ticket3.id,
                    service_date=date.today() - timedelta(days=2),
                    issue_summary="Landing gear misalignment",
                    action_taken="Straightened and reinforced landing leg.",
                    performed_by=engineer.id,
                    status="completed",
                )
            )

        if not db.query(Document).filter(Document.customer_id == customer1.id).first():
            db.add(
                Document(
                    document_type="warranty_certificate",
                    customer_id=customer1.id,
                    drone_id=drone11.id,
                    file_name="TAVAS-0011-warranty-certificate.pdf",
                    file_url="/uploads/seed/tavas-0011-warranty.pdf",
                    uploaded_by=admin.id,
                )
            )
            db.add(
                Document(
                    document_type="service_report",
                    customer_id=customer1.id,
                    drone_id=drone12.id,
                    file_name="TAVAS-0012-service-report.pdf",
                    file_url="/uploads/seed/tavas-0012-service-report.pdf",
                    uploaded_by=engineer.id,
                )
            )

        for type_, title, body in [
            ("ticket_created", "Ticket created", f"Your ticket {ticket2.ticket_number} has been created."),
            (
                "ticket_assigned",
                "Ticket assigned",
                f"Ticket {ticket1.ticket_number} has been assigned to an engineer.",
            ),
            (
                "ticket_status_changed",
                "Ticket status updated",
                f"Ticket {ticket1.ticket_number} is now in progress.",
            ),
            ("invoice_generated", "New invoice", f"Invoice {invoice2.invoice_number} has been generated."),
            ("payment_pending", "Payment pending", f"Invoice {invoice2.invoice_number} is pending payment."),
            ("service_completed", "Service completed", f"Service completed for {drone13.drone_name}."),
        ]:
            db.add(Notification(user_id=user1.id, type=type_, title=title, body=body))

        # ---- Second customer, different company, for isolation testing ----
        user2, _ = make_user(
            db,
            email="owner@skyfleet.example",
            full_name="Meera Iyer",
            role="customer",
            phone="+919844444444",
        )
        customer2 = Customer(
            user_id=user2.id,
            customer_code="NS-CUST-000002",
            company_name="SkyFleet Logistics",
            billing_address="45 Cargo Lane, Bengaluru, KA 560001",
            gstin="29XYZAB5678C1Z2",
        )
        db.add(customer2)
        db.flush()

        drone21, _ = make_drone(
            db,
            customer=customer2,
            drone_name="TAVAS-0021",
            model="TAVAS Mk3",
            serial_number="SN-TAVAS-0021",
            status="active",
            purchase_days_ago=200,
            warranty_days=730,
        )
        make_components(db, drone21)
        make_warranty(db, drone21)
        make_maintenance_schedule(db, drone21, last_maintenance_hours=10.0, last_maintenance_days_ago=30)
        make_flights(db, drone21, user2, count=5)

        db.commit()
        print("Seed data created successfully.\n")
        print_credentials()
    finally:
        db.close()


def print_credentials():
    print("=" * 70)
    print("Seeded login credentials (password for all accounts: NeoSky@123)")
    print("=" * 70)
    print(f"{'Role':<18}{'Email':<32}{'Notes'}")
    print(f"{'customer':<18}{'aniket@throttle.aero':<32}Throttle Aero, 3 drones")
    print(f"{'customer':<18}{'owner@skyfleet.example':<32}SkyFleet Logistics, 1 drone (isolation test)")
    print(f"{'service_engineer':<18}{'engineer@neosky.example':<32}assigned to in-progress ticket")
    print(f"{'admin':<18}{'admin@neosky.example':<32}")
    print("=" * 70)


if __name__ == "__main__":
    main()
