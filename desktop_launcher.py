"""
Entry point for the standalone desktop build (packaged into an .exe via
PyInstaller -- see build_exe.bat / README.md "Desktop app" section).

Two things a plain `python app.py` doesn't handle for a double-click desktop
app:

1. gunicorn (used for the web/Docker deployment) depends on Unix-only OS
   features and does not run on Windows at all. This uses `waitress`
   instead, a pure-Python WSGI server that works on Windows/macOS/Linux.

2. app.py creates its data folders (uploads/, reports_out/, sessions/,
   archive/) as plain relative paths, resolved against the process's
   current working directory. That's fine for `python app.py` run from the
   repo root, but a PyInstaller --onefile .exe unpacks the bundled code
   (templates/, static/, baselines/) into a throwaway temp directory each
   run -- if we let Python's default cwd stay wherever Windows launched the
   .exe from (or the temp bundle itself), the archive/session data would
   either scatter or vanish between runs. So this sets the working directory
   to wherever the .exe itself lives *before* importing app.py, so all of
   that data persists next to the .exe across runs, the way a desktop app
   is expected to behave.
"""

import os
import sys
import threading
import time
import webbrowser


def app_directory():
    if getattr(sys, "frozen", False):
        # Running as a PyInstaller-built .exe: sys.executable is the .exe
        # itself, not a temp-extracted copy, so data saved next to it persists.
        return os.path.dirname(sys.executable)
    return os.path.dirname(os.path.abspath(__file__))


os.chdir(app_directory())

from app import app  # noqa: E402  (must run after the chdir above)

HOST = "127.0.0.1"
PORT = 8080


def open_browser_when_ready():
    time.sleep(1.5)
    webbrowser.open(f"http://{HOST}:{PORT}")


if __name__ == "__main__":
    threading.Thread(target=open_browser_when_ready, daemon=True).start()

    print("=" * 60)
    print(" Neosky Drone Diagnostic Platform")
    print(f" Running at http://{HOST}:{PORT}")
    print(" A browser tab should open automatically.")
    print(" Close this window to stop the app.")
    print("=" * 60)

    from waitress import serve
    serve(app, host=HOST, port=PORT)
