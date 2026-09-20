#!/bin/sh
set -e

echo "Waiting for Postgres at ${DATABASE_URL}..."
python - <<'PYEOF'
import os
import sys
import time

import psycopg2

database_url = os.environ["DATABASE_URL"]
for attempt in range(30):
    try:
        conn = psycopg2.connect(database_url)
        conn.close()
        print("Postgres is up.")
        break
    except Exception as exc:  # noqa: BLE001
        print(f"Postgres not ready yet ({exc}); retrying...")
        time.sleep(2)
else:
    print("Postgres never became ready", file=sys.stderr)
    sys.exit(1)
PYEOF

echo "Running init_db.py (idempotent: no-op if schema already applied)..."
python scripts/init_db.py

echo "Running seed.py (idempotent: no-op if seed data already present)..."
python scripts/seed.py

echo "Starting application: $*"
exec "$@"
