"""create ticket, add comment, admin changes status, customer closes only
after resolved (attempting to close before resolved -> 400/409)."""

from tests.conftest import requires_db, unique_email
from tests.helpers import auth_headers, create_role_user, register_and_login


@requires_db
def test_full_ticket_lifecycle(app_client, db_engine):
    customer_email = unique_email("lifecycle-customer")
    admin_email = unique_email("lifecycle-admin")

    customer_token = register_and_login(app_client, email=customer_email, password="Str0ngPass1!")
    create_role_user(db_engine, email=admin_email, full_name="Lifecycle Admin", role="admin")
    admin_token = app_client.post(
        "/api/auth/login", json={"identifier": admin_email, "password": "NeoSky@123"}
    ).json()["access_token"]

    customer_id = app_client.get("/api/customer/profile", headers=auth_headers(customer_token)).json()["id"]

    drone_resp = app_client.post(
        "/api/admin/drones",
        headers=auth_headers(admin_token),
        json={
            "customer_id": customer_id,
            "drone_name": "Lifecycle-Drone",
            "model": "TAVAS Mk3",
            "serial_number": f"SN-LIFECYCLE-{customer_id[:8]}",
            "status": "active",
        },
    )
    assert drone_resp.status_code == 201
    drone_id = drone_resp.json()["id"]

    ticket_resp = app_client.post(
        "/api/tickets",
        headers=auth_headers(customer_token),
        json={
            "drone_id": drone_id,
            "category": "flight_issue",
            "priority": "high",
            "subject": "Drone won't take off",
            "description": "Motors spin but drone does not lift off.",
        },
    )
    assert ticket_resp.status_code == 201, ticket_resp.text
    ticket = ticket_resp.json()
    assert ticket["status"] == "new"
    assert ticket["ticket_number"].startswith("NS-")
    ticket_id = ticket["id"]

    # Customer adds a comment.
    comment_resp = app_client.post(
        f"/api/tickets/{ticket_id}/comments",
        headers=auth_headers(customer_token),
        json={"comment": "Any update?"},
    )
    assert comment_resp.status_code == 201, comment_resp.text

    # Customer attempts to close a not-yet-resolved ticket -> rejected.
    early_close = app_client.post(
        f"/api/tickets/{ticket_id}/close",
        headers=auth_headers(customer_token),
        json={"confirmed": True},
    )
    assert early_close.status_code in (400, 409)

    # Admin transitions the ticket through the workflow.
    in_progress = app_client.put(
        f"/api/admin/tickets/{ticket_id}/status",
        headers=auth_headers(admin_token),
        json={"status": "in_progress", "comment": "Investigating."},
    )
    assert in_progress.status_code == 200, in_progress.text
    assert in_progress.json()["status"] == "in_progress"

    resolved = app_client.put(
        f"/api/admin/tickets/{ticket_id}/status",
        headers=auth_headers(admin_token),
        json={"status": "resolved", "comment": "Replaced faulty ESC."},
    )
    assert resolved.status_code == 200, resolved.text
    assert resolved.json()["status"] == "resolved"
    assert resolved.json()["resolved_at"] is not None

    # Now the customer can close it.
    close_resp = app_client.post(
        f"/api/tickets/{ticket_id}/close",
        headers=auth_headers(customer_token),
        json={"confirmed": True, "feedback": "All good now, thanks!"},
    )
    assert close_resp.status_code == 200, close_resp.text
    closed = close_resp.json()
    assert closed["status"] == "closed"
    assert closed["customer_confirmed_resolution"] is True
    assert closed["closed_at"] is not None

    # Timeline reflects every status transition in order.
    detail = app_client.get(f"/api/tickets/{ticket_id}", headers=auth_headers(customer_token)).json()
    transitions = [(t["status_from"], t["status_to"]) for t in detail["timeline"]]
    assert ("in_progress", "resolved") in transitions
    assert ("resolved", "closed") in transitions

    # Closing again is rejected (no longer "resolved").
    second_close = app_client.post(
        f"/api/tickets/{ticket_id}/close",
        headers=auth_headers(customer_token),
        json={"confirmed": True},
    )
    assert second_close.status_code in (400, 409)


@requires_db
def test_ticket_assign_updates_status_and_notifies(app_client, db_engine):
    customer_email = unique_email("assign-customer")
    admin_email = unique_email("assign-admin")
    engineer_email = unique_email("assign-engineer")

    customer_token = register_and_login(app_client, email=customer_email, password="Str0ngPass1!")
    create_role_user(db_engine, email=admin_email, full_name="Assign Admin", role="admin")
    engineer_id = create_role_user(
        db_engine, email=engineer_email, full_name="Assign Engineer", role="service_engineer"
    )
    admin_token = app_client.post(
        "/api/auth/login", json={"identifier": admin_email, "password": "NeoSky@123"}
    ).json()["access_token"]

    customer_id = app_client.get("/api/customer/profile", headers=auth_headers(customer_token)).json()["id"]
    drone_id = app_client.post(
        "/api/admin/drones",
        headers=auth_headers(admin_token),
        json={
            "customer_id": customer_id,
            "drone_name": "Assign-Drone",
            "model": "TAVAS Mk3",
            "serial_number": f"SN-ASSIGN-{customer_id[:8]}",
            "status": "active",
        },
    ).json()["id"]

    ticket_id = app_client.post(
        "/api/tickets",
        headers=auth_headers(customer_token),
        json={
            "drone_id": drone_id,
            "category": "connectivity",
            "priority": "low",
            "subject": "GCS disconnects",
            "description": "GCS randomly disconnects mid-flight.",
        },
    ).json()["id"]

    assign_resp = app_client.put(
        f"/api/admin/tickets/{ticket_id}/assign",
        headers=auth_headers(admin_token),
        json={"engineer_id": engineer_id},
    )
    assert assign_resp.status_code == 200, assign_resp.text
    body = assign_resp.json()
    assert body["assigned_engineer_id"] == engineer_id
    assert body["status"] == "assigned"

    notifications = app_client.get("/api/notifications", headers=auth_headers(customer_token)).json()["items"]
    assert any(n["type"] == "ticket_assigned" for n in notifications)
