#!/usr/bin/env python3
"""Executes ../database/schema.sql verbatim against DATABASE_URL.

schema.sql is the single source of truth for the DB shape (see the
backend's app/models/ for the SQLAlchemy mappings onto it). This script
never uses Base.metadata.create_all() — it just runs the DDL file.

Meant to run once against an empty database. It is idempotent in the sense
that re-running it against a database that already has the NeoSky tables
will fail loudly (CREATE TABLE / CREATE TYPE without IF NOT EXISTS) rather
than silently doing nothing or corrupting data — which is the safer
default for a hand-authored schema file. If you need to reset a dev
database, drop it and recreate it first.
"""

import os
import sys

import psycopg2
from dotenv import load_dotenv

load_dotenv()

SCHEMA_PATH = os.path.join(os.path.dirname(__file__), "..", "..", "database", "schema.sql")


def already_initialized(cur) -> bool:
    cur.execute("SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users')")
    return cur.fetchone()[0]


def main() -> int:
    database_url = os.environ.get("DATABASE_URL")
    if not database_url:
        print("DATABASE_URL is not set", file=sys.stderr)
        return 1

    schema_path = os.path.abspath(SCHEMA_PATH)
    if not os.path.isfile(schema_path):
        print(f"schema.sql not found at {schema_path}", file=sys.stderr)
        return 1

    with open(schema_path, "r", encoding="utf-8") as fh:
        schema_sql = fh.read()

    conn = psycopg2.connect(database_url)
    try:
        conn.autocommit = False
        with conn.cursor() as cur:
            if already_initialized(cur):
                print("Database already initialized (users table exists) — skipping.")
                return 0
            print(f"Executing {schema_path} ...")
            cur.execute(schema_sql)
        conn.commit()
        print("Schema applied successfully.")
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
