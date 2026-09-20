import logging

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import or_
from sqlalchemy.orm import Session

from app.core.otp import generate_otp, verify_otp
from app.core.security import (
    create_access_token,
    create_refresh_token,
    decode_token,
    hash_password,
    verify_password,
)
from app.database import get_db
from app.models.customer import Customer
from app.models.user import User
from app.schemas.auth import (
    ForgotPasswordRequest,
    LoginRequest,
    MessageResponse,
    RefreshRequest,
    RefreshResponse,
    RegisterRequest,
    RegisterResponse,
    ResetPasswordRequest,
    TokenResponse,
    VerifyOtpRequest,
    VerifyOtpResponse,
)
from app.config import settings
from app.services.customer_service import generate_customer_code

router = APIRouter(prefix="/api/auth", tags=["auth"])
logger = logging.getLogger("neosky.auth")

ACCESS_TOKEN_EXPIRE_SECONDS = settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60


def _validation_error(field: str, message: str) -> HTTPException:
    return HTTPException(
        status_code=status.HTTP_400_BAD_REQUEST,
        detail={"error": {"code": "VALIDATION_ERROR", "message": message, "fields": {field: message}}},
    )


@router.post("/register", response_model=RegisterResponse, status_code=status.HTTP_201_CREATED)
def register(payload: RegisterRequest, db: Session = Depends(get_db)):
    existing = db.query(User).filter(User.email == payload.email).first()
    if existing is not None:
        raise _validation_error("email", "already registered")
    if payload.phone:
        existing_phone = db.query(User).filter(User.phone == payload.phone).first()
        if existing_phone is not None:
            raise _validation_error("phone", "already registered")

    user = User(
        email=payload.email,
        phone=payload.phone,
        password_hash=hash_password(payload.password),
        role="customer",
        full_name=payload.full_name,
        is_verified=False,
    )
    db.add(user)
    db.flush()

    customer = Customer(
        user_id=user.id,
        customer_code=generate_customer_code(db),
        company_name=payload.company_name,
    )
    db.add(customer)
    db.commit()
    db.refresh(user)

    otp = generate_otp(user.email)
    logger.info("OTP for %s: %s (no SMS/email gateway wired up)", user.email, otp)

    return RegisterResponse(user=user, message="Verification OTP sent")


@router.post("/verify-otp", response_model=VerifyOtpResponse)
def verify_otp_endpoint(payload: VerifyOtpRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.email == payload.email).first()
    if user is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "User not found"}},
        )
    ok = verify_otp(payload.email, payload.otp)
    if not ok:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"error": {"code": "INVALID_OTP", "message": "Invalid or expired OTP"}},
        )
    user.is_verified = True
    db.add(user)
    db.commit()
    return VerifyOtpResponse(verified=True)


@router.post("/login", response_model=TokenResponse)
def login(payload: LoginRequest, db: Session = Depends(get_db)):
    user = (
        db.query(User).filter(or_(User.email == payload.identifier, User.phone == payload.identifier)).first()
    )
    if user is None or not verify_password(payload.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"error": {"code": "INVALID_CREDENTIALS", "message": "Invalid email/phone or password"}},
        )
    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"error": {"code": "ACCOUNT_DISABLED", "message": "Account is disabled"}},
        )

    from datetime import datetime, timezone

    user.last_login_at = datetime.now(timezone.utc)
    db.add(user)
    db.commit()
    db.refresh(user)

    access_token = create_access_token(user)
    refresh_token = create_refresh_token(user)

    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        token_type="bearer",
        expires_in=ACCESS_TOKEN_EXPIRE_SECONDS,
        user=user,
    )


@router.post("/refresh", response_model=RefreshResponse)
def refresh(payload: RefreshRequest, db: Session = Depends(get_db)):
    token_payload = decode_token(payload.refresh_token)
    if token_payload.get("type") != "refresh":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"error": {"code": "INVALID_TOKEN", "message": "Not a refresh token"}},
        )
    import uuid as uuid_mod

    user = db.get(User, uuid_mod.UUID(token_payload["sub"]))
    if user is None or not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"error": {"code": "INVALID_TOKEN", "message": "User not found or inactive"}},
        )
    access_token = create_access_token(user)
    return RefreshResponse(access_token=access_token, expires_in=ACCESS_TOKEN_EXPIRE_SECONDS)


@router.post("/forgot-password", response_model=MessageResponse)
def forgot_password(payload: ForgotPasswordRequest, db: Session = Depends(get_db)):
    user = (
        db.query(User).filter(or_(User.email == payload.identifier, User.phone == payload.identifier)).first()
    )
    # Always respond the same way whether or not the identifier exists, to
    # avoid leaking which emails/phones are registered.
    if user is not None:
        otp = generate_otp(payload.identifier)
        logger.info("Password-reset OTP for %s: %s", payload.identifier, otp)
    return MessageResponse(message="OTP sent")


@router.post("/reset-password", response_model=MessageResponse)
def reset_password(payload: ResetPasswordRequest, db: Session = Depends(get_db)):
    user = (
        db.query(User).filter(or_(User.email == payload.identifier, User.phone == payload.identifier)).first()
    )
    if user is None or not verify_otp(payload.identifier, payload.otp):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"error": {"code": "INVALID_OTP", "message": "Invalid or expired OTP"}},
        )
    user.password_hash = hash_password(payload.new_password)
    db.add(user)
    db.commit()
    return MessageResponse(message="Password updated")


@router.post("/logout", status_code=status.HTTP_204_NO_CONTENT)
def logout():
    # Refresh tokens are stateless signed JWTs (see app/core/security.py for
    # why schema.sql leaves no table to persist server-side revocation), so
    # there is nothing to invalidate server-side; the client discards both
    # tokens. Returns 204 either way.
    return None
