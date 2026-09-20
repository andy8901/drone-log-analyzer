"""Notification creation + a stub push-notification call to FCM."""

import logging
import uuid

import requests
from sqlalchemy.orm import Session

from app.config import settings
from app.models.notification import Notification
from app.models.user import User

logger = logging.getLogger("neosky.notifications")


def send_push(fcm_token: str | None, title: str, body: str) -> None:
    """Best-effort push via FCM's legacy HTTP send endpoint. Never raises —
    a push-delivery failure must not fail the request that triggered it."""
    if not fcm_token or not settings.FCM_SERVER_KEY:
        return
    try:
        requests.post(
            settings.FCM_SEND_URL,
            headers={
                "Authorization": f"key={settings.FCM_SERVER_KEY}",
                "Content-Type": "application/json",
            },
            json={
                "to": fcm_token,
                "notification": {"title": title, "body": body},
            },
            timeout=5,
        )
    except Exception:  # noqa: BLE001 - push failures must never break the request
        logger.exception("FCM push failed for token=%s", fcm_token)


def create_notification(
    db: Session,
    *,
    user: User,
    type_: str,
    title: str,
    body: str,
    related_entity_type: str | None = None,
    related_entity_id: uuid.UUID | None = None,
) -> Notification:
    notification = Notification(
        user_id=user.id,
        type=type_,
        title=title,
        body=body,
        related_entity_type=related_entity_type,
        related_entity_id=related_entity_id,
    )
    db.add(notification)
    db.flush()
    send_push(user.fcm_token, title, body)
    return notification
