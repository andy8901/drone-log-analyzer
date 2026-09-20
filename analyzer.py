import os
import glob
import math
from collections import defaultdict
from pymavlink import mavutil


# =========================================================
# ARDUPILOT REFERENCE TABLES
# =========================================================

# ArduPilot's LogErrorSubsystem enum (libraries/AP_Logger/AP_Logger.h).
# Stable across firmware versions; used to decode the dataflash "ERR" message.
ERROR_SUBSYSTEMS = {
    1: "MAIN", 2: "RADIO", 3: "COMPASS", 4: "OPTFLOW",
    5: "RADIO FAILSAFE", 6: "BATTERY FAILSAFE", 7: "GPS FAILSAFE",
    8: "GCS FAILSAFE", 9: "FENCE FAILSAFE", 10: "FLIGHT MODE",
    11: "GPS", 12: "CRASH CHECK", 13: "FLIP", 14: "AUTOTUNE",
    15: "PARACHUTE", 16: "EKF CHECK", 17: "EKF/INAV FAILSAFE",
    18: "BAROMETER", 19: "CPU", 20: "ADSB FAILSAFE", 21: "TERRAIN",
    22: "NAVIGATION", 23: "TERRAIN FAILSAFE", 24: "EKF PRIMARY",
    25: "THRUST LOSS CHECK", 26: "SENSORS FAILSAFE", 27: "LEAK FAILSAFE",
    28: "PILOT INPUT", 29: "VIBRATION FAILSAFE", 30: "INTERNAL ERROR",
    31: "DEAD RECKONING FAILSAFE",
}

# Subsystems whose errors are treated as flight-safety CRITICAL rather than WARNING.
CRITICAL_SUBSYSTEMS = {5, 6, 7, 8, 9, 12, 13, 16, 17, 25, 26, 27, 29, 30, 31}

# Safety/tuning-relevant parameter prefixes checked against a drone-model baseline.
# (OSD layout, RC channel mapping, servo functions etc. are intentionally excluded
# since they vary legitimately per airframe/build.)
CRITICAL_PARAM_PREFIXES = (
    "EK3_", "BATT_", "FS_", "ARMING_", "ATC_RAT_", "ATC_ANG_",
    "INS_GYRO_FILTER", "INS_ACCEL_FILTER", "INS_HNTCH_",
    "COMPASS_USE", "GPS_HDOP_GOOD", "FENCE_", "MOT_PWM_", "MOT_SPIN_",
    "RTL_ALT", "THR_DZ", "LAND_SPEED", "PSC_",
)

BASELINE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "baselines")


# =========================================================
# HELPER FUNCTIONS
# =========================================================

def calculate_deviation(actual, standard):
    if not standard:
        return 0
    return round(((actual - standard) / standard) * 100, 2)


def rms(data):
    if not data:
        return 0
    return round(math.sqrt(sum(x * x for x in data) / len(data)), 2)


def gps_distance(lat1, lon1, lat2, lon2):
    return math.sqrt((lat2 - lat1) ** 2 + (lon2 - lon1) ** 2) * 111139


def get_stats(data_list, std=0):
    if not data_list:
        return {"min": 0, "max": 0, "avg": 0, "dev": 0}

    avg = sum(data_list) / len(data_list)

    return {
        "min": round(min(data_list), 2),
        "max": round(max(data_list), 2),
        "avg": round(avg, 2),
        "dev": calculate_deviation(avg, std),
    }


def _decimation_indices(n, max_points):
    if n <= max_points or n == 0:
        return list(range(n))
    step = n / max_points
    idx = [int(i * step) for i in range(max_points)]
    idx[-1] = n - 1
    return idx


def downsample(xs, ys, max_points=1200):
    """Evenly decimate a paired time series so large logs stay light in the browser/report."""
    idx = _decimation_indices(len(xs), max_points)
    return [xs[i] for i in idx], [ys[i] for i in idx]


def downsample_multi(t, *series, max_points=1200):
    """Decimate a timestamp array plus one or more parallel value arrays together."""
    idx = _decimation_indices(len(t), max_points)
    return ([t[i] for i in idx],) + tuple([s[i] for i in idx] for s in series)


def decimate_list(items, max_points=2000):
    idx = _decimation_indices(len(items), max_points)
    return [items[i] for i in idx]


def find_baseline_file(drone_model):
    if not drone_model:
        return None
    for path in glob.glob(os.path.join(BASELINE_DIR, "*.param")):
        name = os.path.splitext(os.path.basename(path))[0]
        if name.strip().lower() == drone_model.strip().lower():
            return path
    return None


def load_baseline_params(path):
    baseline = {}
    with open(path, "r", errors="ignore") as f:
        for line in f:
            line = line.strip()
            if not line or "," not in line:
                continue
            name, _, value = line.partition(",")
            try:
                baseline[name.strip()] = float(value.strip())
            except ValueError:
                continue
    return baseline


def compare_to_baseline(params_dump, drone_model):
    """Flag safety/tuning parameters that drifted from the drone's baseline .param file."""
    path = find_baseline_file(drone_model)

    if not path:
        return None, []

    baseline = load_baseline_params(path)
    deviations = []

    for name, base_val in baseline.items():
        if not name.startswith(CRITICAL_PARAM_PREFIXES):
            continue

        actual_val = params_dump.get(name)
        if actual_val is None:
            continue

        try:
            actual_val = float(actual_val)
        except (TypeError, ValueError):
            continue

        tolerance = max(abs(base_val) * 0.05, 1e-3)

        if abs(actual_val - base_val) > tolerance:
            deviations.append({
                "param": name,
                "baseline": round(base_val, 6),
                "actual": round(actual_val, 6),
            })

    return os.path.basename(path), deviations


def build_conclusion(status, alerts, summary, param_deviations, drone_model):
    lines = []

    duration = summary["duration"]

    if status == "PASS":
        lines.append(
            f"Flight of {duration} min completed with no critical anomalies across "
            f"battery, GPS, IMU/vibration, and attitude-control telemetry."
        )
    elif status == "WARNING":
        lines.append(
            f"Flight of {duration} min completed, but telemetry shows warning-level "
            f"anomalies that should be reviewed before the next sortie."
        )
    else:
        lines.append(
            f"Flight of {duration} min shows CRITICAL anomalies that likely affected "
            f"flight safety or performance. Grounding and inspection are recommended "
            f"before the next flight."
        )

    root_cause_hints = [
        ("THRUST", "Inspect motors, ESCs, and propellers immediately — thrust loss can lead to loss of control."),
        ("VIBRATION", "Inspect propellers for damage/imbalance, check motor mounts and frame arms for looseness, and verify propeller clearance from the frame."),
        ("CLIPPING", "Accelerometer clipping indicates excessive shock/vibration reaching the flight controller — check vibration isolation/mounting."),
        ("GPS", "Check GPS antenna mounting/shielding, and prefer open-sky locations if satellite count was consistently low."),
        ("BATTERY", "Inspect battery health/internal resistance; consider retiring the pack if voltage sag exceeds the expected baseline."),
        ("EKF", "Review compass, GPS, and vibration health — EKF instability commonly follows compass interference or high vibration."),
        ("COMPASS", "Check for magnetic interference from nearby power wiring or metal, and consider redoing the compass calibration."),
        ("ATTITUDE", "Review PID tuning (ATC_RAT_* gains) and check for mechanical binding, prop damage, or CG imbalance."),
    ]

    alert_text = " ".join(alerts)
    for keyword, hint in root_cause_hints:
        if keyword in alert_text:
            lines.append(f"{keyword.title()}: {hint}")

    if param_deviations:
        lines.append(
            f"{len(param_deviations)} safety-relevant parameter(s) differ from the "
            f"{drone_model} baseline by more than the expected tolerance — verify these "
            f"were intentional tuning changes before the next flight."
        )

    return lines


# =========================================================
# MAIN LOG PROCESSOR
# =========================================================

def process_log(file_path, drone_model=None):

    log = mavutil.mavlink_connection(file_path)

    tele = defaultdict(list)

    params_dump = {}
    events_log = []
    alerts = []
    flight_modes = []
    gps_path = []

    start_ts = None
    end_ts = None

    ekf_errors = 0
    clip_counts = [0, 0, 0]

    def rel_time(ts):
        return round((ts - start_ts) / 1000000, 3) if start_ts is not None else 0

    # =====================================================
    # LOG LOOP
    # =====================================================

    while True:

        msg = log.recv_match()

        if msg is None:
            break

        d = msg.to_dict()
        m_type = msg.get_type()

        if "TimeUS" in d:
            if start_ts is None:
                start_ts = d["TimeUS"]
            end_ts = d["TimeUS"]

        # ---------------- PARAMETERS ----------------

        if m_type == "PARM":
            name = d.get("Name")
            if name:
                params_dump[name.strip()] = d.get("Value")

        # ---------------- ERRORS ----------------

        elif m_type == "ERR":
            subsys = d.get("Subsys")
            code = d.get("ECode", d.get("Code", 0))
            name = ERROR_SUBSYSTEMS.get(subsys, f"SUBSYSTEM {subsys}")

            if code == 0:
                events_log.append(f"[ERR] {name} RESOLVED")
            else:
                severity = "CRITICAL" if subsys in CRITICAL_SUBSYSTEMS else "WARNING"
                alerts.append(f"{severity}: {name} ERROR (code {code})")

                if subsys == 16:
                    ekf_errors += 1

        # ---------------- BATTERY ----------------

        elif m_type in ["BAT", "BATTERY_STATUS"]:
            volt = d.get("Volt")
            curr = d.get("Curr")

            if volt is None:
                voltages = d.get("voltages", [0])
                volt = (voltages[0] / 1000) if voltages else 0

            if curr is None:
                curr = d.get("current_battery", 0) / 100

            tele["volt"].append(volt)
            tele["curr"].append(curr)

            if "TimeUS" in d:
                tele["bat_time"].append(rel_time(d["TimeUS"]))

        # ---------------- GPS ----------------

        elif m_type in ["GPS", "GPS_RAW_INT"]:

            if "Alt" in d:
                alt = d["Alt"]  # dataflash GPS.Alt is already metres
            else:
                alt = d.get("alt", 0) / 1000.0  # MAVLink GPS_RAW_INT.alt is millimetres

            tele["alt_gps"].append(alt)

            if "TimeUS" in d:
                tele["gps_time"].append(rel_time(d["TimeUS"]))

            lat = d.get("Lat", d.get("lat"))
            lon = d.get("Lng", d.get("lon"))

            if lat and lon:

                if abs(lat) > 1000:
                    lat = lat / 1e7

                if abs(lon) > 1000:
                    lon = lon / 1e7

                gps_path.append([lat, lon])
                tele["lat"].append(lat)
                tele["lon"].append(lon)

            sats = d.get("NSats", d.get("satellites_visible"))
            if sats:
                tele["sats"].append(sats)

            hdop = d.get("HDop")
            if hdop:
                tele["hdop"].append(hdop)

        # ---------------- BAROMETER ----------------

        elif m_type == "BARO":
            alt = d.get("Alt", 0)
            tele["alt_baro"].append(alt)

            if "TimeUS" in d:
                tele["baro_time"].append(rel_time(d["TimeUS"]))

        # ---------------- IMU (gyro noise, supplementary to VIBE) ----------------

        elif m_type in ["IMU", "RAW_IMU"]:
            tele["gyr_x"].append(d.get("GyrX", d.get("xgyro", 0)))
            tele["gyr_y"].append(d.get("GyrY", d.get("ygyro", 0)))
            tele["gyr_z"].append(d.get("GyrZ", d.get("zgyro", 0)))

        # ---------------- VIBE (actual ArduPilot vibration metric) ----------------

        elif m_type == "VIBE":
            tele["vibe_x"].append(d.get("VibeX", 0))
            tele["vibe_y"].append(d.get("VibeY", 0))
            tele["vibe_z"].append(d.get("VibeZ", 0))

            if "TimeUS" in d:
                tele["vibe_time"].append(rel_time(d["TimeUS"]))

            clip_counts = [
                d.get("Clip0", clip_counts[0]),
                d.get("Clip1", clip_counts[1]),
                d.get("Clip2", clip_counts[2]),
            ]

        # ---------------- MAGNETOMETER ----------------

        elif m_type == "MAG":
            tele["mag_x"].append(d.get("MagX", 0))
            tele["mag_y"].append(d.get("MagY", 0))
            tele["mag_z"].append(d.get("MagZ", 0))

        # ---------------- ATTITUDE (actual vs desired ANGLE, degrees) ----------------

        elif m_type == "ATT":
            roll = d.get("Roll", 0)
            pitch = d.get("Pitch", 0)
            yaw = d.get("Yaw", 0)

            tele["roll_act"].append(roll)
            tele["pitch_act"].append(pitch)
            tele["yaw_act"].append(yaw)

            tele["roll_des"].append(d.get("DesRoll", roll))
            tele["pitch_des"].append(d.get("DesPitch", pitch))
            tele["yaw_des"].append(d.get("DesYaw", yaw))

            if "ErrRP" in d:
                tele["err_rp"].append(d["ErrRP"])

            if "TimeUS" in d:
                tele["att_time"].append(rel_time(d["TimeUS"]))

        # ---------------- RATE (rate-loop tracking, supplementary) ----------------

        elif m_type == "RATE":
            tele["roll_rate_act"].append(d.get("R", 0))
            tele["roll_rate_des"].append(d.get("RDes", d.get("R", 0)))
            tele["pitch_rate_act"].append(d.get("P", 0))
            tele["pitch_rate_des"].append(d.get("PDes", d.get("P", 0)))
            tele["yaw_rate_act"].append(d.get("Y", 0))
            tele["yaw_rate_des"].append(d.get("YDes", d.get("Y", 0)))

        # ---------------- FLIGHT MODES ----------------

        elif m_type == "MODE":
            mode = d.get("Mode")
            flight_modes.append(mode)
            events_log.append(f"MODE CHANGED → {mode}")

        # ---------------- EVENTS / SYSTEM MESSAGES ----------------

        elif m_type in ["MSG", "EV"]:
            txt = d.get("Message", d.get("Text", "Event"))
            events_log.append(f"[{m_type}] {txt}")

    # =====================================================
    # FLIGHT STATISTICS
    # =====================================================

    duration_min = round((end_ts - start_ts) / 60000000, 2) if start_ts and end_ts else 0

    voltage_drop = round(max(tele["volt"]) - min(tele["volt"]), 2) if tele["volt"] else 0
    min_volt = round(min(tele["volt"]), 2) if tele["volt"] else 0
    avg_current = round(sum(tele["curr"]) / len(tele["curr"]), 2) if tele["curr"] else 0
    max_current = round(max(tele["curr"]), 2) if tele["curr"] else 0

    max_altitude = round(max(tele["alt_gps"]), 2) if tele["alt_gps"] else 0

    avg_sats = round(sum(tele["sats"]) / len(tele["sats"]), 2) if tele["sats"] else 0
    avg_hdop = round(sum(tele["hdop"]) / len(tele["hdop"]), 2) if tele["hdop"] else 0

    # ---------------- VIBRATION ----------------

    if tele["vibe_x"]:
        max_vibe = round(max(max(tele["vibe_x"]), max(tele["vibe_y"]), max(tele["vibe_z"])), 2)
        vibration_source = "VIBE"
    else:
        # Older logs / MAVLink telemetry logs without a VIBE message: fall back to
        # gyro-noise RMS as a rough proxy (not the same physical quantity as VIBE m/s/s).
        gyro_rms_x = rms(tele["gyr_x"])
        gyro_rms_y = rms(tele["gyr_y"])
        gyro_rms_z = rms(tele["gyr_z"])
        max_vibe = max(gyro_rms_x, gyro_rms_y, gyro_rms_z)
        vibration_source = "GYRO_RMS"

    # ---------------- ATTITUDE ERROR ----------------

    roll_error = max(
        (abs(a - dd) for a, dd in zip(tele["roll_act"], tele["roll_des"])),
        default=0,
    )
    pitch_error = max(
        (abs(a - dd) for a, dd in zip(tele["pitch_act"], tele["pitch_des"])),
        default=0,
    )

    # ---------------- GPS JUMP DETECTION ----------------

    for i in range(1, len(gps_path)):
        dist = gps_distance(
            gps_path[i - 1][0], gps_path[i - 1][1],
            gps_path[i][0], gps_path[i][1],
        )
        if dist > 50:
            alerts.append("GPS POSITION JUMP DETECTED")
            break

    # =====================================================
    # ALERT METRICS
    # =====================================================

    # GPS
    if avg_sats and avg_sats < 7:
        alerts.append("CRITICAL GPS SIGNAL LOSS")
    elif avg_sats and avg_sats < 10:
        alerts.append("LOW GPS SATELLITES")

    hdop_good_param = params_dump.get("GPS_HDOP_GOOD")
    hdop_good = (hdop_good_param / 100.0) if hdop_good_param else 2.0

    if avg_hdop > hdop_good * 1.5:
        alerts.append("POOR GPS HDOP")
    elif avg_hdop > hdop_good:
        alerts.append("HIGH GPS HDOP")

    # BATTERY — relative sag over the flight
    if voltage_drop > 4:
        alerts.append("CRITICAL BATTERY SAG")
    elif voltage_drop > 2.5:
        alerts.append("HIGH BATTERY SAG")

    # BATTERY — absolute thresholds from the log's own PARM dump, when available
    batt_crt_volt = params_dump.get("BATT_CRT_VOLT")
    batt_low_volt = params_dump.get("BATT_LOW_VOLT")

    if min_volt and batt_crt_volt and min_volt <= batt_crt_volt:
        alerts.append(f"CRITICAL BATTERY VOLTAGE ({min_volt}V at/below BATT_CRT_VOLT {batt_crt_volt}V)")
    elif min_volt and batt_low_volt and min_volt <= batt_low_volt:
        alerts.append(f"LOW BATTERY VOLTAGE ({min_volt}V at/below BATT_LOW_VOLT {batt_low_volt}V)")

    # CURRENT
    if max_current > 50:
        alerts.append("CRITICAL CURRENT DRAW")
    elif max_current > 30:
        alerts.append("HIGH CURRENT DRAW")

    # VIBRATION
    if max_vibe > 60:
        alerts.append("CRITICAL VIBRATION")
    elif max_vibe > 30:
        alerts.append("HIGH VIBRATION")

    if any(clip_counts):
        total_clips = sum(clip_counts)
        if total_clips > 100:
            alerts.append(f"CRITICAL ACCELEROMETER CLIPPING ({total_clips} events)")
        else:
            alerts.append(f"ACCELEROMETER CLIPPING ({total_clips} events)")

    # EKF
    if ekf_errors > 5:
        alerts.append("CRITICAL EKF FAILURE")
    elif ekf_errors > 2:
        alerts.append("EKF INSTABILITY")

    # ATTITUDE
    if roll_error > 30 or pitch_error > 30:
        alerts.append("CRITICAL ATTITUDE INSTABILITY")
    elif roll_error > 15 or pitch_error > 15:
        alerts.append("HIGH ATTITUDE ERROR")

    # COMPASS
    if tele["mag_x"]:
        mag_range = max(tele["mag_x"]) - min(tele["mag_x"])
        if mag_range > 300:
            alerts.append("CRITICAL COMPASS INTERFERENCE")
        elif mag_range > 150:
            alerts.append("HIGH MAGNETIC INTERFERENCE")

    # ---------------- BASELINE PARAMETER COMPARISON ----------------

    baseline_file, param_deviations = compare_to_baseline(params_dump, drone_model)

    if param_deviations:
        alerts.append(f"{len(param_deviations)} PARAMETER(S) DEVIATE FROM BASELINE")

    # =====================================================
    # STATUS LOGIC
    # =====================================================

    critical_keywords = ["CRITICAL", "THRUST"]
    warning_keywords = ["HIGH", "LOW", "POOR", "INSTABILITY", "WARNING", "DEVIATE", "CLIPPING"]

    alert_text = " ".join(alerts)

    if any(k in alert_text for k in critical_keywords):
        status = "CRITICAL"
    elif any(k in alert_text for k in warning_keywords):
        status = "WARNING"
    else:
        status = "PASS"

    # =====================================================
    # TELEMETRY DETAILS TABLE
    # =====================================================

    details = [
        {
            "cat": "1.0 Flight",
            "param": f"Flight Duration ({duration_min})",
            "min": "-", "max": duration_min, "avg": "-", "dev": "-",
        },
        {"cat": "1.0 Flight", "param": "GPS Altitude (M)", "std": 50, **get_stats(tele["alt_gps"], 50)},
        {"cat": "1.0 Flight", "param": "Barometer Altitude (M)", "std": 50, **get_stats(tele["alt_baro"], 50)},

        {"cat": "2.0 Battery", "param": "Voltage Drop (V)", "std": 1.5, **get_stats([voltage_drop], 1.5)},
        {"cat": "2.0 Battery", "param": "Average Current (A)", "std": 20, **get_stats(tele["curr"], 20)},
        {"cat": "2.0 Battery", "param": "Peak Current (A)", "std": 30, **get_stats([max_current], 30)},

        {"cat": "3.0 GPS", "param": "Average Satellites", "std": 15, **get_stats(tele["sats"], 15)},
        {"cat": "3.0 GPS", "param": "HDOP", "std": hdop_good, **get_stats(tele["hdop"], hdop_good)},

        {"cat": "4.0 Vibration", "param": f"Max Vibration ({vibration_source})", "std": 30, **get_stats([max_vibe], 30)},
        {"cat": "4.0 Vibration", "param": "Accelerometer Clip Count", "min": "-", "max": sum(clip_counts), "avg": "-", "dev": "-"},

        {"cat": "5.0 Magnetometer", "param": "Mag X", "std": 0, **get_stats(tele["mag_x"])},
        {"cat": "5.0 Magnetometer", "param": "Mag Y", "std": 0, **get_stats(tele["mag_y"])},
        {"cat": "5.0 Magnetometer", "param": "Mag Z", "std": 0, **get_stats(tele["mag_z"])},

        {"cat": "6.0 Attitude", "param": "Roll Angle Error (deg)", "std": 5, **get_stats([roll_error], 5)},
        {"cat": "6.0 Attitude", "param": "Pitch Angle Error (deg)", "std": 5, **get_stats([pitch_error], 5)},
    ]

    if tele["err_rp"]:
        details.append({"cat": "6.0 Attitude", "param": "AHRS Roll/Pitch Error (ErrRP)", "std": 0, **get_stats(tele["err_rp"])})

    if tele["roll_rate_act"]:
        rate_roll_err = max(
            (abs(a - dd) for a, dd in zip(tele["roll_rate_act"], tele["roll_rate_des"])), default=0,
        )
        rate_pitch_err = max(
            (abs(a - dd) for a, dd in zip(tele["pitch_rate_act"], tele["pitch_rate_des"])), default=0,
        )
        details.append({"cat": "7.0 Rate Controller", "param": "Roll Rate Error (deg/s)", "std": 0, **get_stats([rate_roll_err])})
        details.append({"cat": "7.0 Rate Controller", "param": "Pitch Rate Error (deg/s)", "std": 0, **get_stats([rate_pitch_err])})

    # =====================================================
    # CHART-READY TIME SERIES (downsampled for the browser)
    # =====================================================

    charts = {}

    bt, volt = downsample(tele["bat_time"], tele["volt"])
    charts["battery"] = {"t": bt, "volt": volt}

    bt2, curr = downsample(tele["bat_time"], tele["curr"])
    charts["current"] = {"t": bt2, "curr": curr}

    bat, alt_b = downsample(tele["baro_time"], tele["alt_baro"])
    charts["altitude_baro"] = {"t": bat, "alt": alt_b}

    gat, alt_g = downsample(tele["gps_time"], tele["alt_gps"])
    charts["altitude_gps"] = {"t": gat, "alt": alt_g}

    if tele["att_time"]:
        at, roll_a, roll_d = downsample_multi(tele["att_time"], tele["roll_act"], tele["roll_des"])
        _, pitch_a, pitch_d = downsample_multi(tele["att_time"], tele["pitch_act"], tele["pitch_des"])
        charts["attitude_roll"] = {"t": at, "actual": roll_a, "desired": roll_d}
        charts["attitude_pitch"] = {"t": at, "actual": pitch_a, "desired": pitch_d}
    else:
        charts["attitude_roll"] = {"t": [], "actual": [], "desired": []}
        charts["attitude_pitch"] = {"t": [], "actual": [], "desired": []}

    if tele["vibe_x"]:
        vt, vx, vy, vz = downsample_multi(tele["vibe_time"], tele["vibe_x"], tele["vibe_y"], tele["vibe_z"])
        charts["vibration"] = {"t": vt, "x": vx, "y": vy, "z": vz}
    else:
        charts["vibration"] = {"t": [], "x": [], "y": [], "z": []}

    display_gps_path = decimate_list(gps_path, max_points=2000)

    conclusion = build_conclusion(status, alerts, {
        "duration": duration_min,
    }, param_deviations, drone_model)

    # =====================================================
    # RETURN
    # =====================================================

    return {
        "filename": os.path.basename(file_path),
        "status": status,
        "alerts": list(dict.fromkeys(alerts)),  # de-duplicate, keep order
        "details": details,
        "events": events_log,
        "params": params_dump,
        "gps_path": display_gps_path,
        "flight_modes": list(dict.fromkeys(flight_modes)),
        "charts": charts,
        "conclusion": conclusion,
        "baseline_file": baseline_file,
        "param_deviations": param_deviations,

        "summary": {
            "duration": duration_min,
            "max_altitude": max_altitude,
            "avg_current": avg_current,
            "max_current": max_current,
            "voltage_drop": voltage_drop,
            "min_voltage": min_volt,
            "avg_sats": avg_sats,
            "avg_hdop": avg_hdop,
            "max_vibration": max_vibe,
            "vibration_source": vibration_source,
        },
    }
