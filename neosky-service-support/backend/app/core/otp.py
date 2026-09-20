"""Ephemeral OTP store.

schema.sql (the authoritative, unmodifiable DB contract) has no table for
OTP codes, so this cannot be persisted to the database. It is kept as an
in-process store, which is adequate for a stub verification flow (no real
SMS/email gateway is wired up either) but means OTPs do not survive a
process restart and are not shared across multiple worker processes. This
is a deliberate, documented deviation forced by the fixed schema.
"""

import random
import time

_OTP_TTL_SECONDS = 10 * 60
_store: dict[str, tuple[str, float]] = {}


def generate_otp(identifier: str) -> str:
    code = f"{random.randint(0, 999999):06d}"
    _store[identifier] = (code, time.time() + _OTP_TTL_SECONDS)
    return code


def verify_otp(identifier: str, code: str) -> bool:
    entry = _store.get(identifier)
    if entry is None:
        return False
    stored_code, expires_at = entry
    if time.time() > expires_at:
        _store.pop(identifier, None)
        return False
    if stored_code != code:
        return False
    _store.pop(identifier, None)
    return True
