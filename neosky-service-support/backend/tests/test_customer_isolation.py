"""THE most important test: customer A creates/owns a drone and ticket;
customer B must get 404 (not the data, not 403) when requesting them."""

from tests.conftest import requires_db, unique_email
from tests.helpers import auth_headers, create_role_user, register_and_login


@requires_db
def test_customer_b_cannot_access_customer_a_drone_or_ticket(app_client, db_engine):
    email_a = unique_email("customer-a")
    email_b = unique_email("customer-b")
    admin_email = unique_email("admin")

    token_a = register_and_login(app_client, email=email_a, password="Str0ngPass1!", full_name="Customer A")
    token_b = register_and_login(app_client, email=email_b, password="Str0ngPass2!", full_name="Customer B")
    create_role_user(db_engine, email=admin_email, full_name="Admin User", role="admin")
    admin_token = app_client.post(
        "/api/auth/login", json={"identifier": admin_email, "password": "NeoSky@123"}
    ).json()["access_token"]

    profile_a = app_client.get("/api/customer/profile", headers=auth_headers(token_a)).json()
    customer_a_id = profile_a["id"]

    drone_resp = app_client.post(
        "/api/admin/drones",
        headers=auth_headers(admin_token),
        json={
            "customer_id": customer_a_id,
            "drone_name": "Isolation-Test-Drone",
            "model": "TAVAS Mk3",
            "serial_number": f"SN-ISO-{customer_a_id[:8]}",
            "status": "active",
        },
    )
    assert drone_resp.status_code == 201, drone_resp.text
    drone_a_id = drone_resp.json()["id"]

    ticket_resp = app_client.post(
        "/api/tickets",
        headers=auth_headers(token_a),
        json={
            "drone_id": drone_a_id,
            "category": "hardware_issue",
            "priority": "medium",
            "subject": "Isolation test ticket",
            "description": "Should not be visible to customer B.",
        },
    )
    assert ticket_resp.status_code == 201, ticket_resp.text
    ticket_a_id = ticket_resp.json()["id"]

    # Customer A can see their own resources.
    assert app_client.get(f"/api/drones/{drone_a_id}", headers=auth_headers(token_a)).status_code == 200
    assert app_client.get(f"/api/tickets/{ticket_a_id}", headers=auth_headers(token_a)).status_code == 200

    # Customer B must get 404 — not 403, not the actual data — for both.
    drone_as_b = app_client.get(f"/api/drones/{drone_a_id}", headers=auth_headers(token_b))
    assert drone_as_b.status_code == 404, drone_as_b.text
    assert drone_as_b.json()["error"]["code"] == "NOT_FOUND"

    ticket_as_b = app_client.get(f"/api/tickets/{ticket_a_id}", headers=auth_headers(token_b))
    assert ticket_as_b.status_code == 404, ticket_as_b.text
    assert ticket_as_b.json()["error"]["code"] == "NOT_FOUND"

    # Customer B's own (empty) lists must not include customer A's rows either.
    b_drones = app_client.get("/api/drones", headers=auth_headers(token_b)).json()
    assert all(d["id"] != drone_a_id for d in b_drones)

    b_tickets = app_client.get("/api/tickets", headers=auth_headers(token_b)).json()["items"]
    assert all(t["id"] != ticket_a_id for t in b_tickets)


@requires_db
def test_customer_cannot_access_admin_routes(app_client):
    email = unique_email("plain-customer")
    token = register_and_login(app_client, email=email, password="Str0ngPass1!")
    resp = app_client.get("/api/admin/customers", headers=auth_headers(token))
    assert resp.status_code == 403
