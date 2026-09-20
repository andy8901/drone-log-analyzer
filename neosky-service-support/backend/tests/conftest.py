"""Shared pytest fixtures.

DB-dependent tests need TEST_DATABASE_URL set to a reachable, disposable
Postgres database (the schema is (re)applied fresh for each test session).
If it's unset, those tests are skipped cleanly via `requires_db`.
"""

import os
import subprocess
import sys
import uuid

import pytest
from dotenv import load_dotenv

load_dotenv()

TEST_DATABASE_URL = os.environ.get("TEST_DATABASE_URL")

requires_db = pytest.mark.skipif(
    not TEST_DATABASE_URL, reason="TEST_DATABASE_URL is not set; skipping DB-dependent test"
)

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))


@pytest.fixture(scope="session")
def db_engine():
    if not TEST_DATABASE_URL:
        pytest.skip("TEST_DATABASE_URL is not set")

    import psycopg2

    # Drop & recreate a clean public schema, then apply schema.sql fresh.
    conn = psycopg2.connect(TEST_DATABASE_URL)
    conn.autocommit = True
    with conn.cursor() as cur:
        cur.execute("DROP SCHEMA public CASCADE;")
        cur.execute("CREATE SCHEMA public;")
    conn.close()

    env = os.environ.copy()
    env["DATABASE_URL"] = TEST_DATABASE_URL
    result = subprocess.run(
        [sys.executable, os.path.join(REPO_ROOT, "scripts", "init_db.py")],
        env=env,
        capture_output=True,
        text=True,
    )
    assert result.returncode == 0, f"init_db.py failed:\n{result.stdout}\n{result.stderr}"

    from sqlalchemy import create_engine

    engine = create_engine(TEST_DATABASE_URL, future=True)
    yield engine
    engine.dispose()


@pytest.fixture()
def db_session(db_engine):
    from sqlalchemy.orm import sessionmaker

    connection = db_engine.connect()
    trans = connection.begin()
    Session = sessionmaker(bind=connection, future=True)
    session = Session()
    try:
        yield session
    finally:
        session.close()
        trans.rollback()
        connection.close()


@pytest.fixture()
def app_client(db_engine, monkeypatch):
    """A TestClient wired to the test database, with each test isolated by
    truncating all tables before it runs (simplest reliable isolation given
    FastAPI's own session-per-request pattern)."""
    os.environ["DATABASE_URL"] = TEST_DATABASE_URL

    from sqlalchemy import text

    with db_engine.begin() as conn:
        tables = (
            conn.execute(text("SELECT tablename FROM pg_tables WHERE schemaname = 'public'")).scalars().all()
        )
        if tables:
            conn.execute(text(f"TRUNCATE TABLE {', '.join(tables)} RESTART IDENTITY CASCADE"))

    # Reset the app's module-level engine to point at the test DB.
    import importlib

    import app.database as app_database

    importlib.reload(app_database)
    app_database.engine.dispose()
    from sqlalchemy import create_engine
    from sqlalchemy.orm import sessionmaker

    app_database.engine = create_engine(TEST_DATABASE_URL, future=True)
    app_database.SessionLocal = sessionmaker(bind=app_database.engine, future=True)

    import app.main as app_main

    importlib.reload(app_main)

    from fastapi.testclient import TestClient

    with TestClient(app_main.app) as client:
        yield client


def unique_email(prefix: str) -> str:
    return f"{prefix}-{uuid.uuid4().hex[:8]}@example.com"
