"""Test helpers shared across DB-backed test modules."""

from sqlalchemy.orm import sessionmaker

from app.core.security import hash_password
from app.models.user import User


def create_role_user(
    db_engine, *, email: str, full_name: str, role: str, password: str = "NeoSky@123"
) -> str:
    """Inserts and commits a user with an arbitrary role directly (there is
    no public API to create service_engineer/admin accounts — by design,
    only customers can self-register). Returns the user id as a string."""
    Session = sessionmaker(bind=db_engine, future=True)
    session = Session()
    try:
        user = User(
            email=email,
            password_hash=hash_password(password),
            role=role,
            full_name=full_name,
            is_active=True,
            is_verified=True,
        )
        session.add(user)
        session.commit()
        return str(user.id)
    finally:
        session.close()


def register_and_login(
    client, *, email: str, password: str, full_name: str = "Test User", company_name: str = "Test Co"
):
    register_resp = client.post(
        "/api/auth/register",
        json={"full_name": full_name, "email": email, "password": password, "company_name": company_name},
    )
    assert register_resp.status_code == 201, register_resp.text

    login_resp = client.post("/api/auth/login", json={"identifier": email, "password": password})
    assert login_resp.status_code == 200, login_resp.text
    return login_resp.json()["access_token"]


def auth_headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}"}
