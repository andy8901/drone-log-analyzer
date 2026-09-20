"""Pure unit tests for password hashing and JWT handling. No DB needed."""

import uuid
from datetime import timedelta

import pytest
from fastapi import HTTPException
from jose import jwt

from app.config import settings
from app.core.security import (
    TOKEN_TYPE_ACCESS,
    TOKEN_TYPE_REFRESH,
    _create_token,
    create_access_token,
    create_refresh_token,
    decode_token,
    hash_password,
    verify_password,
)


class FakeUser:
    def __init__(self):
        self.id = uuid.uuid4()
        self.role = "customer"


def test_hash_password_roundtrip():
    plain = "S3cur3P@ssw0rd!"
    hashed = hash_password(plain)
    assert hashed != plain
    assert verify_password(plain, hashed) is True


def test_hash_password_rejects_wrong_password():
    hashed = hash_password("correct-horse-battery-staple")
    assert verify_password("wrong-password", hashed) is False


def test_hash_password_never_returns_plaintext_and_is_salted():
    plain = "same-password"
    hash1 = hash_password(plain)
    hash2 = hash_password(plain)
    assert hash1 != hash2  # bcrypt salts each hash differently
    assert verify_password(plain, hash1)
    assert verify_password(plain, hash2)


def test_create_and_decode_access_token():
    user = FakeUser()
    token = create_access_token(user)
    payload = decode_token(token)
    assert payload["sub"] == str(user.id)
    assert payload["type"] == TOKEN_TYPE_ACCESS
    assert payload["role"] == "customer"


def test_create_and_decode_refresh_token():
    user = FakeUser()
    token = create_refresh_token(user)
    payload = decode_token(token)
    assert payload["sub"] == str(user.id)
    assert payload["type"] == TOKEN_TYPE_REFRESH


def test_decode_expired_token_raises_401():
    user = FakeUser()
    expired_token = _create_token(
        subject=str(user.id), token_type=TOKEN_TYPE_ACCESS, expires_delta=timedelta(seconds=-1)
    )
    with pytest.raises(HTTPException) as excinfo:
        decode_token(expired_token)
    assert excinfo.value.status_code == 401


def test_decode_tampered_token_raises_401():
    user = FakeUser()
    token = create_access_token(user)
    tampered = token[:-2] + ("aa" if not token.endswith("aa") else "bb")
    with pytest.raises(HTTPException) as excinfo:
        decode_token(tampered)
    assert excinfo.value.status_code == 401


def test_decode_wrong_secret_raises_401():
    bad_token = jwt.encode(
        {"sub": "x", "type": "access"}, "a-completely-different-secret", algorithm=settings.JWT_ALGORITHM
    )
    with pytest.raises(HTTPException) as excinfo:
        decode_token(bad_token)
    assert excinfo.value.status_code == 401
