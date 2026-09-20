import os

import pandas as pd
from docx import Document
from docx.shared import Inches, Pt
from docx.enum.text import WD_ALIGN_PARAGRAPH

LOGO_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "static", "logo.png")


def generate_word_report(report_data, mission_info, output_path):
    doc = Document()

    if os.path.exists(LOGO_PATH):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.add_run().add_picture(LOGO_PATH, width=Inches(1.8))

    doc.add_heading("Neosky India Ltd - Flight Log Analysis", 0)

    summary = report_data.get("summary", {})

    doc.add_heading("1.0 Mission Details", level=1)
    table = doc.add_table(rows=11, cols=2)
    table.style = "Table Grid"
    info = [
        ("Drone Model:", mission_info.get("drone")),
        ("MSN Number:", mission_info.get("msn")),
        ("Incident Date:", mission_info.get("date")),
        ("Reported By:", mission_info.get("reported_by")),
        ("File Name:", report_data["filename"]),
        ("Health Status:", report_data["status"]),
        ("Flight Duration (armed):", f"{summary.get('flight_duration')} min ({summary.get('flight_duration_source')})"),
        ("Log/Session Duration:", f"{summary.get('session_duration')} min"),
        ("Initial / Final Voltage:", f"{summary.get('initial_voltage')}V / {summary.get('final_voltage')}V"),
        ("Camera:", f"{report_data.get('camera', {}).get('photo_count', 0)} photo trigger(s) logged"),
        (
            "Farthest Range from Home:",
            f"{summary.get('max_range_m')} m (home = first GPS fix at "
            f"{summary.get('home_lat')}, {summary.get('home_lon')})"
            if summary.get("home_lat") is not None else "No GPS fix in log",
        ),
    ]
    for i, (k, v) in enumerate(info):
        table.rows[i].cells[0].text, table.rows[i].cells[1].text = k, str(v)

    doc.add_heading("2.0 Conclusion", level=1)
    for line in report_data.get("conclusion", []):
        doc.add_paragraph(line, style="List Bullet")

    if report_data.get("alert_categories"):
        doc.add_heading("3.0 Alerts by Category", level=1)
        for category, items in report_data["alert_categories"].items():
            doc.add_heading(category, level=2)
            for alert in items:
                p = doc.add_paragraph(f" {alert}")
                p.runs[0].bold = True

    if report_data.get("param_deviations"):
        doc.add_heading(
            f"4.0 Parameter Deviations vs Baseline ({report_data.get('baseline_file')})",
            level=1,
        )
        t = doc.add_table(rows=1, cols=3)
        t.style = "Table Grid"
        for i, txt in enumerate(["Parameter", "Baseline", "Actual"]):
            t.rows[0].cells[i].text = txt
        for dev in report_data["param_deviations"]:
            row = t.add_row().cells
            row[0].text, row[1].text, row[2].text = (
                dev["param"], str(dev["baseline"]), str(dev["actual"]),
            )

    doc.add_heading("5.0 Telemetry Summary", level=1)
    current_cat = ""
    t = None
    for item in report_data["details"]:
        if item["cat"] != current_cat:
            current_cat = item["cat"]
            doc.add_heading(current_cat, level=2)
            t = doc.add_table(rows=1, cols=5)
            t.style = "Table Grid"
            for i, txt in enumerate(["Parameter", "Min", "Max", "Avg", "Dev%"]):
                t.rows[0].cells[i].text = txt

        row = t.add_row().cells
        row[0].text, row[1].text, row[2].text, row[3].text, row[4].text = (
            item["param"], str(item["min"]), str(item["max"]),
            str(item["avg"]), f"{item['dev']}%",
        )

    if report_data.get("timeline"):
        doc.add_page_break()
        doc.add_heading("6.0 Event Timeline (modes, errors, messages)", level=1)
        t = doc.add_table(rows=1, cols=3)
        t.style = "Table Grid"
        for i, txt in enumerate(["Time (mm:ss)", "Type", "Message"]):
            t.rows[0].cells[i].text = txt
        for e in report_data["timeline"]:
            row = t.add_row().cells
            row[0].text, row[1].text, row[2].text = e.get("t_min", str(e["t"])), e["type"], e["text"]

    doc.add_page_break()
    doc.add_heading("Appendix: Full Parameter Dump", level=1)
    p_table = doc.add_table(rows=1, cols=2)
    p_table.style = "Table Grid"
    for name, val in report_data["params"].items():
        r = p_table.add_row().cells
        r[0].text, r[1].text = str(name), str(val)

    doc.save(output_path)


def generate_excel(report_data, output_path):
    with pd.ExcelWriter(output_path) as writer:
        summary = report_data.get("summary", {})
        pd.DataFrame(
            list(summary.items()), columns=["Metric", "Value"]
        ).to_excel(writer, sheet_name="Summary", index=False)

        pd.DataFrame(report_data["details"]).to_excel(writer, sheet_name="Telemetry", index=False)

        pd.DataFrame(
            {"Conclusion": report_data.get("conclusion", [])}
        ).to_excel(writer, sheet_name="Conclusion", index=False)

        alert_rows = [
            {"Category": cat, "Alert": alert}
            for cat, items in report_data.get("alert_categories", {}).items()
            for alert in items
        ]
        pd.DataFrame(alert_rows, columns=["Category", "Alert"]).to_excel(
            writer, sheet_name="Alerts", index=False
        )

        if report_data.get("param_deviations"):
            pd.DataFrame(report_data["param_deviations"]).to_excel(
                writer, sheet_name="Baseline Deviations", index=False
            )

        pd.DataFrame(
            list(report_data["params"].items()), columns=["Parameter", "Value"]
        ).to_excel(writer, sheet_name="Parameters", index=False)

        timeline_df = pd.DataFrame(report_data.get("timeline", []))
        if not timeline_df.empty:
            timeline_df = timeline_df[["t_min", "type", "text", "t"]].rename(
                columns={"t_min": "Time (mm:ss)", "type": "Type", "text": "Message", "t": "Time (s)"}
            )
        timeline_df.to_excel(writer, sheet_name="Timeline", index=False)
