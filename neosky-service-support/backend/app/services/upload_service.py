"""File upload validation & storage.

Uploaded files are stored under settings.UPLOAD_DIR (outside the served
static/web root) and referenced by an opaque path; in production this
would be swapped for object storage with signed URLs, but the interface
(`save_upload`) stays the same.
"""

import os
import uuid

from fastapi import HTTPException, UploadFile, status

from app.config import settings

ALLOWED_EXTENSIONS = {
    ".jpg",
    ".jpeg",
    ".png",
    ".gif",
    ".webp",  # photo
    ".mp4",
    ".mov",
    ".avi",  # video
    ".pdf",
    ".doc",
    ".docx",
    ".xls",
    ".xlsx",
    ".txt",  # document
    ".log",
    ".csv",
    ".bin",
    ".ulg",
    ".tlog",  # log
}
ALLOWED_CONTENT_TYPE_PREFIXES = ("image/", "video/", "application/", "text/")


def save_upload(file: UploadFile, subdir: str) -> tuple[str, int]:
    """Validates and persists `file` under UPLOAD_DIR/subdir. Returns
    (relative_file_url, size_bytes)."""
    ext = os.path.splitext(file.filename or "")[1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"error": {"code": "VALIDATION_ERROR", "message": f"file extension {ext!r} not allowed"}},
        )
    content_type = file.content_type or ""
    if not content_type.startswith(ALLOWED_CONTENT_TYPE_PREFIXES):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={
                "error": {"code": "VALIDATION_ERROR", "message": f"content type {content_type!r} not allowed"}
            },
        )

    contents = file.file.read()
    if len(contents) > settings.MAX_UPLOAD_SIZE_BYTES:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"error": {"code": "VALIDATION_ERROR", "message": "file too large"}},
        )

    target_dir = os.path.join(settings.UPLOAD_DIR, subdir)
    os.makedirs(target_dir, exist_ok=True)
    stored_name = f"{uuid.uuid4()}{ext}"
    target_path = os.path.join(target_dir, stored_name)
    with open(target_path, "wb") as fh:
        fh.write(contents)

    relative_url = f"/uploads/{subdir}/{stored_name}"
    return relative_url, len(contents)
