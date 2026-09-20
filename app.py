import json
import os
import shutil
from datetime import datetime, timezone

from flask import Flask, render_template, request, send_file
from werkzeug.utils import secure_filename
from uuid import uuid4

from analyzer import process_log
from reports import generate_excel, generate_word_report

app = Flask(__name__)

UPLOAD_FOLDER = 'uploads'
REPORT_FOLDER = 'reports_out'
SESSION_FOLDER = 'sessions'
ARCHIVE_FOLDER = 'archive/logs'
MANIFEST_PATH = 'archive/manifest.jsonl'

app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['MAX_CONTENT_LENGTH'] = 200 * 1024 * 1024

os.makedirs(UPLOAD_FOLDER, exist_ok=True)
os.makedirs(REPORT_FOLDER, exist_ok=True)
os.makedirs(SESSION_FOLDER, exist_ok=True)
os.makedirs(ARCHIVE_FOLDER, exist_ok=True)

ALLOWED_EXTENSIONS = {'bin', 'log', 'tlog'}

# In-process cache for the common case (same worker serves the export right
# after the upload). Every result is also written to disk so exports still
# work when a different gunicorn worker, or a fresh instance after a
# restart/redeploy, handles the /export request.
session_cache = {}


def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS


def session_path(report_id):
    return os.path.join(SESSION_FOLDER, f"{report_id}.json")


def save_session(report_id, data):
    session_cache[report_id] = data
    with open(session_path(report_id), "w") as f:
        json.dump(data, f)


def load_session(report_id):
    if report_id in session_cache:
        return session_cache[report_id]

    path = session_path(report_id)
    if not os.path.exists(path):
        return None

    with open(path) as f:
        data = json.load(f)

    session_cache[report_id] = data
    return data


def archive_log(upload_path, filename, report_id, mission, report):
    """Every uploaded log is kept permanently, alongside a manifest entry
    recording its outcome ('cause'), so nothing is lost even if the person
    never downloads the Word/Excel report."""
    archived_name = f"{report_id}_{filename}"
    archived_path = os.path.join(ARCHIVE_FOLDER, archived_name)

    try:
        shutil.move(upload_path, archived_path)
    except OSError:
        app.logger.warning("Could not archive upload %s", upload_path, exc_info=True)
        archived_path = None

    entry = {
        "report_id": report_id,
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "original_filename": filename,
        "archived_path": archived_path,
        "mission": mission,
        "status": report["status"],
        "cause": report["alerts"][:5] if report["alerts"] else ["No anomalies detected"],
        "conclusion": report.get("conclusion", []),
    }

    try:
        with open(MANIFEST_PATH, "a") as f:
            f.write(json.dumps(entry) + "\n")
    except OSError:
        app.logger.warning("Could not write manifest entry for %s", report_id, exc_info=True)


@app.route("/", methods=["GET", "POST"])
def index():

    if request.method == "POST":

        mission = {
            "drone": request.form.get('drone'),
            "msn": request.form.get('msn'),
            "date": request.form.get('date'),
            "reported_by": request.form.get('reported_by')
        }

        file = request.files.get('log_file')

        if not file or not file.filename:
            return "No file selected", 400

        if not allowed_file(file.filename):
            return "Invalid file type", 400

        filename = secure_filename(file.filename)

        upload_path = os.path.join(
            app.config['UPLOAD_FOLDER'],
            filename
        )

        file.save(upload_path)

        try:
            report = process_log(upload_path, drone_model=mission.get('drone'))

            report_id = str(uuid4())

            data = {
                "report": report,
                "mission": mission
            }

            save_session(report_id, data)
            archive_log(upload_path, filename, report_id, mission, report)

            return render_template(
                "dashboard.html",
                data=data,
                report_id=report_id
            )

        except Exception as e:
            app.logger.exception("Log processing failed for %s", upload_path)
            return f"Processing Failed: {str(e)}", 500

        finally:
            # archive_log() moves the file on success; this only cleans up
            # if processing failed before the move happened.
            try:
                if os.path.exists(upload_path):
                    os.remove(upload_path)
            except OSError:
                app.logger.warning("Could not remove upload %s", upload_path, exc_info=True)

    return render_template("dashboard.html", data=None)


@app.route("/export/<report_id>/<fmt>")
def export(report_id, fmt):

    data = load_session(report_id)

    if not data:
        return "No report found", 404

    ext = 'docx' if fmt == 'word' else 'xlsx'

    filename = f"Neosky_Report_{report_id}.{ext}"

    path = os.path.join(REPORT_FOLDER, filename)

    if fmt == 'word':
        generate_word_report(
            data['report'],
            data['mission'],
            path
        )
    else:
        generate_excel(
            data['report'],
            path
        )

    return send_file(path, as_attachment=True)


if __name__ == "__main__":
    debug = os.environ.get("FLASK_DEBUG", "0") == "1"
    port = int(os.environ.get("PORT", 8080))
    app.run(host="0.0.0.0", port=port, debug=debug)
