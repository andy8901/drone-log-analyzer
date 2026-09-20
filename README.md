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
you a public URL works. Two easy options:

### Render (recommended for a quick free link)

1. Push this repo to GitHub (already done).
2. On [render.com](https://render.com) → **New +** → **Web Service** → connect
   this repo.
3. Render auto-detects the `Dockerfile`. Leave the port unset (the app reads
   `PORT` from the environment automatically).
4. Deploy — Render gives you a public `https://<name>.onrender.com` URL.

### Railway / Fly.io / any Docker host

- **Railway**: New Project → Deploy from GitHub repo → it detects the
  `Dockerfile` and gives you a public URL.
- **Fly.io**: `fly launch` in this directory, accept the detected
  `Dockerfile`, then `fly deploy`.
- **Any VPS**: `docker build -t drone-log-analyzer . && docker run -p 8080:8080 drone-log-analyzer`.

### Notes for production

- Uploaded logs are deleted right after processing; nothing is retained on
  disk beyond the JSON report needed to serve the "Download Word/Excel"
  buttons (stored in `sessions/`).
- `reports_out/`, `uploads/`, and `sessions/` are gitignored — don't commit
  generated reports or uploaded logs.
- There's no authentication — anyone with the link can upload logs and
  generate reports. Add auth in front of it (e.g. your host's built-in basic
  auth, or a proxy) if the link will be shared outside a trusted group.
