"""register -> login -> refresh -> access a protected route."""

from tests.conftest import requires_db, unique_email


@requires_db
def test_register_login_refresh_and_protected_route(app_client):
    email = unique_email("regflow")
    register_resp = app_client.post(
        "/api/auth/register",
        json={
            "full_name": "Test User",
            "email": email,
            "phone": None,
            "password": "Str0ngPass!",
            "company_name": "Test Co",
        },
    )
    assert register_resp.status_code == 201, register_resp.text
    body = register_resp.json()
    assert body["user"]["email"] == email
    assert body["user"]["role"] == "customer"

    login_resp = app_client.post("/api/auth/login", json={"identifier": email, "password": "Str0ngPass!"})
    assert login_resp.status_code == 200, login_resp.text
    tokens = login_resp.json()
    assert "access_token" in tokens
    assert "refresh_token" in tokens
    assert tokens["expires_in"] == 900

    refresh_resp = app_client.post("/api/auth/refresh", json={"refresh_token": tokens["refresh_token"]})
    assert refresh_resp.status_code == 200, refresh_resp.text
    new_access_token = refresh_resp.json()["access_token"]

    profile_resp = app_client.get(
        "/api/customer/profile", headers={"Authorization": f"Bearer {new_access_token}"}
    )
    assert profile_resp.status_code == 200, profile_resp.text
    assert profile_resp.json()["email"] == email


@requires_db
def test_login_with_wrong_password_returns_401(app_client):
    email = unique_email("badpass")
    app_client.post(
        "/api/auth/register",
        json={"full_name": "Bad Pass", "email": email, "password": "CorrectPass1!"},
    )
    resp = app_client.post("/api/auth/login", json={"identifier": email, "password": "WrongPass1!"})
    assert resp.status_code == 401


@requires_db
def test_protected_route_without_token_returns_401(app_client):
    resp = app_client.get("/api/customer/profile")
    assert resp.status_code == 401


@requires_db
def test_duplicate_registration_returns_400(app_client):
    email = unique_email("dupe")
    payload = {"full_name": "Dupe User", "email": email, "password": "Str0ngPass!"}
    first = app_client.post("/api/auth/register", json=payload)
    assert first.status_code == 201
    second = app_client.post("/api/auth/register", json=payload)
    assert second.status_code == 400
    assert second.json()["error"]["code"] == "VALIDATION_ERROR"
