# Neosky Drone Diagnostic Platform

A Flask web app for uploading ArduPilot flight logs (`.bin` / `.log` / `.tlog`)
and getting back a health diagnostic: pass/warning/critical status, root-cause
alerts, time-series graphs, a flight-path replay, and downloadable Word/Excel
reports.

## What it checks

- **Battery**: voltage sag, absolute low/critical voltage (read from the log's
  own `BATT_LOW_VOLT`/`BATT_CRT_VOLT` parameters when present), current draw.
- **GPS**: satellite count, HDOP (against `GPS_HDOP_GOOD`), position jumps.
- **Vibration**: the ArduPilot `VIBE` message (VibeX/Y/Z + accelerometer clip
  counts), with a gyro-noise fallback for logs that don't have it.
- **EKF / failsafes / crash detection**: decodes the dataflash `ERR` message
  using ArduPilot's `LogErrorSubsystem` table (EKF check, GCS/GPS/battery
  failsafe, thrust loss check, crash check, etc.).
- **Attitude tracking**: actual vs. desired roll/pitch/yaw (from `ATT`) and
  rate-loop tracking (from `RATE`).
- **Compass interference**.
- **Parameter drift**: compares the log's own parameter dump against a
  per-airframe baseline `.param` file in `baselines/`, flagging safety/tuning
  parameters (`EK3_*`, `BATT_*`, `FS_*`, `ARMING_*`, `ATC_RAT_*`, etc.) that
  drifted beyond tolerance.

Each run also produces a plain-language **Conclusion** with root-cause hints
tied to whatever alerts fired.

## Adding a baseline for a drone model

Drop an ArduPilot `.param` file at `baselines/<MODEL>.param`, where
`<MODEL>` matches the "Drone Model" dropdown value (case-insensitive), e.g.
`baselines/TAVAS.param`. Logs uploaded for that model are automatically
compared against it.

## Running locally

```bash
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python app.py
```

Open http://localhost:8080

## Deploying it so you can share a link

The app is packaged with a production `Dockerfile` (Gunicorn, binds to the
`PORT` env var, debug off) — any host that can build a Docker image and give
you a public URL works.

### Render (one-click, via the included Blueprint)

[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/andy8901/drone-log-analyzer)

`render.yaml` in this repo is a [Render Blueprint](https://render.com/docs/blueprint-spec):
clicking the button above (or **New +** → **Blueprint** on
[render.com](https://render.com) → connect this repo) builds the existing
`Dockerfile` with nothing else to configure, and gives you a public
`https://<name>.onrender.com` URL. Deploying requires your own Render account
— this is a link for you to click, not something that can be done on your
behalf without your login.

**Read this before relying on it for real incident data:** the default
`render.yaml` uses Render's free plan, which has an *ephemeral* filesystem —
everything written at runtime, including `archive/logs/` and
`archive/manifest.jsonl` (meant to hold every uploaded log permanently, see
below), is wiped on every deploy, restart, or free-plan spin-down. To make
the archive actually persistent, attach a Render Disk mounted at
`/app/archive` (requires upgrading to the paid "Starter" plan or higher) —
see the commented-out example at the bottom of `render.yaml`.

### Railway / Fly.io / any Docker host

- **Railway**: New Project → Deploy from GitHub repo → it detects the
  `Dockerfile` and gives you a public URL.
- **Fly.io**: `fly launch` in this directory, accept the detected
  `Dockerfile`, then `fly deploy`.
- **Any VPS**: `docker build -t drone-log-analyzer . && docker run -p 8080:8080 drone-log-analyzer`.

These have the same ephemeral-storage caveat as Render's free plan unless you
attach persistent storage (a Railway Volume, a Fly.io Volume, or just a real
disk on a VPS).

### Notes for production

- Every uploaded log is archived permanently under `archive/logs/`, with a
  `archive/manifest.jsonl` entry recording its outcome (status, top alerts,
  full conclusion) — see the persistent-storage note above for what that
  requires on your chosen host.
- `reports_out/`, `uploads/`, `sessions/`, and `archive/` are gitignored —
  don't commit generated reports, uploaded logs, or the archive.
- There's no authentication — anyone with the link can upload logs and
  generate reports. Add auth in front of it (e.g. your host's built-in basic
  auth, or a proxy) if the link will be shared outside a trusted group.
