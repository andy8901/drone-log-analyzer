"""SQLAlchemy engine/session setup.

NOTE: models never create or alter schema (no Base.metadata.create_all()).
The database shape comes exclusively from ../database/schema.sql, executed
once by scripts/init_db.py. The ORM only ever *maps* to tables that already
exist.
"""

from collections.abc import Generator

from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from app.config import settings

engine = create_engine(settings.DATABASE_URL, pool_pre_ping=True, future=True)

SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False, future=True)


class Base(DeclarativeBase):
    pass


def get_db() -> Generator[Session, None, None]:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
