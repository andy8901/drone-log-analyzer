import os
import math
from collections import defaultdict
from pymavlink import mavutil


# =========================================================
# HELPER FUNCTIONS
# =========================================================

def calculate_deviation(actual, standard):

    if not standard or standard == 0:
        return 0

    return round(
        ((actual - standard) / standard) * 100,
        2
    )


def rms(data):

    if not data:
        return 0

    return round(
        math.sqrt(
            sum(x * x for x in data) / len(data)
        ),
        2
    )


def gps_distance(lat1, lon1, lat2, lon2):

    return math.sqrt(
        (lat2 - lat1) ** 2 +
        (lon2 - lon1) ** 2
    ) * 111139


def get_stats(data_list, std=0):

    if not data_list:

        return {
            "min": 0,
            "max": 0,
            "avg": 0,
            "dev": 0
        }

    avg = sum(data_list) / len(data_list)

    return {

        "min": round(min(data_list), 2),

        "max": round(max(data_list), 2),

        "avg": round(avg, 2),

        "dev": calculate_deviation(avg, std)
    }


# =========================================================
# MAIN LOG PROCESSOR
# =========================================================

def process_log(file_path):

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

    # =====================================================
    # LOG LOOP
    # =====================================================

    while True:

        msg = log.recv_match()

        if msg is None:
            break

        d = msg.to_dict()

        m_type = msg.get_type()

        # =================================================
        # TIMESTAMP
        # =================================================

        if 'TimeUS' in d:

            if start_ts is None:
                start_ts = d['TimeUS']

            end_ts = d['TimeUS']

        # =================================================
        # PARAMETERS
        # =================================================

        if m_type == "PARM":

            params_dump[d.get('Name')] = d.get('Value')

        # =================================================
        # ERRORS
        # =================================================

        elif m_type == "ERR":

            subsys = d.get('Subsys')

            if subsys == 25:

                alerts.append(
                    "CRITICAL THRUST LOSS DETECTED"
                )

            elif subsys == 16:

                ekf_errors += 1

                alerts.append(
                    "EKF ERROR DETECTED"
                )

            elif subsys == 30:

                alerts.append(
                    "CRITICAL VIBRATION DETECTED"
                )

        # =================================================
        # BATTERY
        # =================================================

        elif m_type in ["BAT", "BATTERY_STATUS"]:

            tele["volt"].append(
                d.get('Volt', d.get('voltages', [0])[0] / 1000)
            )

            tele["curr"].append(
                d.get('Curr', d.get('current_battery', 0) / 100)
            )

        # =================================================
        # GPS
        # =================================================

        elif m_type in ["GPS", "GPS_RAW_INT"]:

            alt = d.get('Alt', d.get('alt', 0))

            tele["alt_gps"].append(alt)

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

            sats = d.get(
                "NSats",
                d.get("satellites_visible")
            )

            if sats:
                tele["sats"].append(sats)

            hdop = d.get("HDop")

            if hdop:
                tele["hdop"].append(hdop)

        # =================================================
        # BAROMETER
        # =================================================

        elif m_type == "BARO":

            tele["alt_baro"].append(
                d.get('Alt', 0)
            )

        # =================================================
        # IMU
        # =================================================

        elif m_type in ["IMU", "RAW_IMU"]:

            tele["gyr_x"].append(
                d.get('GyrX', d.get('xgyro', 0))
            )

            tele["gyr_y"].append(
                d.get('GyrY', d.get('ygyro', 0))
            )

            tele["gyr_z"].append(
                d.get('GyrZ', d.get('zgyro', 0))
            )

        # =================================================
        # MAGNETOMETER
        # =================================================

        elif m_type == "MAG":

            tele["mag_x"].append(
                d.get('MagX', 0)
            )

            tele["mag_y"].append(
                d.get('MagY', 0)
            )

            tele["mag_z"].append(
                d.get('MagZ', 0)
            )

        # =================================================
        # ATTITUDE
        # =================================================

        elif m_type == "ATT":

            tele["roll_act"].append(
                d.get('Roll', 0)
            )

            tele["pitch_act"].append(
                d.get('Pitch', 0)
            )

            tele["yaw_act"].append(
                d.get('Yaw', 0)
            )

        # =================================================
        # DESIRED RATES
        # =================================================

        elif m_type == "RATE":

            tele["roll_des"].append(
                d.get('DesRoll', 0)
            )

            tele["pitch_des"].append(
                d.get('DesPitch', 0)
            )

            tele["yaw_des"].append(
                d.get('DesYaw', 0)
            )

        # =================================================
        # FLIGHT MODES
        # =================================================

        elif m_type == "MODE":

            mode = d.get('Mode')

            flight_modes.append(mode)

            events_log.append(
                f"MODE CHANGED → {mode}"
            )

        # =================================================
        # EVENTS / SYSTEM MESSAGES
        # =================================================

        elif m_type in ["MSG", "EV"]:

            txt = d.get(
                'Message',
                d.get('Text', 'Event')
            )

            events_log.append(
                f"[{m_type}] {txt}"
            )

    # =====================================================
    # FLIGHT STATISTICS
    # =====================================================

    duration_min = round(
        (end_ts - start_ts) / 60000000,
        2
    ) if start_ts and end_ts else 0

    voltage_drop = round(
        max(tele["volt"]) - min(tele["volt"]),
        2
    ) if tele["volt"] else 0

    avg_current = round(
        sum(tele["curr"]) / len(tele["curr"]),
        2
    ) if tele["curr"] else 0

    max_current = round(
        max(tele["curr"]),
        2
    ) if tele["curr"] else 0

    max_altitude = round(
        max(tele["alt_gps"]),
        2
    ) if tele["alt_gps"] else 0

    avg_sats = round(
        sum(tele["sats"]) / len(tele["sats"]),
        2
    ) if tele["sats"] else 0

    avg_hdop = round(
        sum(tele["hdop"]) / len(tele["hdop"]),
        2
    ) if tele["hdop"] else 0

    # =====================================================
    # VIBRATION ANALYSIS
    # =====================================================

    gyro_rms_x = rms(tele["gyr_x"])
    gyro_rms_y = rms(tele["gyr_y"])
    gyro_rms_z = rms(tele["gyr_z"])

    max_vibe = max(
        gyro_rms_x,
        gyro_rms_y,
        gyro_rms_z
    )

    # =====================================================
    # ATTITUDE ERROR
    # =====================================================

    roll_error = max([

        abs(a - d)

        for a, d in zip(
            tele["roll_act"],
            tele["roll_des"]
        )

    ], default=0)

    pitch_error = max([

        abs(a - d)

        for a, d in zip(
            tele["pitch_act"],
            tele["pitch_des"]
        )

    ], default=0)

    # =====================================================
    # GPS JUMP DETECTION
    # =====================================================

    for i in range(1, len(gps_path)):

        dist = gps_distance(

            gps_path[i - 1][0],
            gps_path[i - 1][1],

            gps_path[i][0],
            gps_path[i][1]
        )

        if dist > 50:

            alerts.append(
                "GPS POSITION JUMP DETECTED"
            )

            break

    # =====================================================
    # ALERT METRICS
    # =====================================================

    # GPS

    if avg_sats < 7:

        alerts.append(
            "CRITICAL GPS SIGNAL LOSS"
        )

    elif avg_sats < 10:

        alerts.append(
            "LOW GPS SATELLITES"
        )

    # HDOP

    if avg_hdop > 2.5:

        alerts.append(
            "POOR GPS HDOP"
        )

    # BATTERY

    if voltage_drop > 4:

        alerts.append(
            "CRITICAL BATTERY SAG"
        )

    elif voltage_drop > 2.5:

        alerts.append(
            "HIGH BATTERY SAG"
        )

    # CURRENT

    if max_current > 50:

        alerts.append(
            "CRITICAL CURRENT DRAW"
        )

    elif max_current > 30:

        alerts.append(
            "HIGH CURRENT DRAW"
        )

    # VIBRATION

    if max_vibe > 40:

        alerts.append(
            "CRITICAL VIBRATION"
        )

    elif max_vibe > 20:

        alerts.append(
            "HIGH VIBRATION"
        )

    # EKF

    if ekf_errors > 5:

        alerts.append(
            "CRITICAL EKF FAILURE"
        )

    elif ekf_errors > 2:

        alerts.append(
            "EKF INSTABILITY"
        )

    # ATTITUDE

    if roll_error > 30 or pitch_error > 30:

        alerts.append(
            "CRITICAL ATTITUDE INSTABILITY"
        )

    elif roll_error > 15 or pitch_error > 15:

        alerts.append(
            "HIGH ATTITUDE ERROR"
        )

    # COMPASS

    if tele["mag_x"]:

        mag_range = max(
            tele["mag_x"]
        ) - min(
            tele["mag_x"]
        )

        if mag_range > 300:

            alerts.append(
                "CRITICAL COMPASS INTERFERENCE"
            )

        elif mag_range > 150:

            alerts.append(
                "HIGH MAGNETIC INTERFERENCE"
            )

    # =====================================================
    # STATUS LOGIC
    # =====================================================

    critical_keywords = [

        "CRITICAL",
        "THRUST",
        "EKF FAILURE"
    ]

    warning_keywords = [

        "HIGH",
        "LOW",
        "POOR",
        "INSTABILITY"
    ]

    alert_text = " ".join(alerts)

    if any(k in alert_text for k in critical_keywords):

        status = "CRITICAL"

    elif any(k in alert_text for k in warning_keywords):

        status = "WARNING"

    else:

        status = "PASS"

    # =====================================================
    # TELEMETRY DETAILS
    # =====================================================

    details = [

        # ================================================
        # FLIGHT
        # ================================================

        {
            "cat": "1.0 Flight",
            "param": "Flight Duration (Min)",
            "std": 15,
            **get_stats([duration_min], 15)
        },

        {
            "cat": "1.0 Flight",
            "param": "GPS Altitude (M)",
            "std": 50,
            **get_stats(tele["alt_gps"], 50)
        },

        {
            "cat": "1.0 Flight",
            "param": "Barometer Altitude (M)",
            "std": 50,
            **get_stats(tele["alt_baro"], 50)
        },

        # ================================================
        # BATTERY
        # ================================================

        {
            "cat": "2.0 Battery",
            "param": "Voltage Drop (V)",
            "std": 1.5,
            **get_stats([voltage_drop], 1.5)
        },

        {
            "cat": "2.0 Battery",
            "param": "Average Current (A)",
            "std": 20,
            **get_stats(tele["curr"], 20)
        },

        {
            "cat": "2.0 Battery",
            "param": "Peak Current (A)",
            "std": 30,
            **get_stats([max_current], 30)
        },

        # ================================================
        # GPS
        # ================================================

        {
            "cat": "3.0 GPS",
            "param": "Average Satellites",
            "std": 15,
            **get_stats(tele["sats"], 15)
        },

        {
            "cat": "3.0 GPS",
            "param": "HDOP",
            "std": 1,
            **get_stats(tele["hdop"], 1)
        },

        # ================================================
        # IMU
        # ================================================

        {
            "cat": "4.0 IMU",
            "param": "Gyro X RMS",
            "std": 10,
            **get_stats([gyro_rms_x], 10)
        },

        {
            "cat": "4.0 IMU",
            "param": "Gyro Y RMS",
            "std": 10,
            **get_stats([gyro_rms_y], 10)
        },

        {
            "cat": "4.0 IMU",
            "param": "Gyro Z RMS",
            "std": 10,
            **get_stats([gyro_rms_z], 10)
        },

        # ================================================
        # MAG
        # ================================================

        {
            "cat": "5.0 Magnetometer",
            "param": "Mag X",
            "std": 0,
            **get_stats(tele["mag_x"])
        },

        {
            "cat": "5.0 Magnetometer",
            "param": "Mag Y",
            "std": 0,
            **get_stats(tele["mag_y"])
        },

        {
            "cat": "5.0 Magnetometer",
            "param": "Mag Z",
            "std": 0,
            **get_stats(tele["mag_z"])
        },

        # ================================================
        # ATTITUDE
        # ================================================

        {
            "cat": "6.0 Attitude",
            "param": "Roll Error",
            "std": 5,
            **get_stats([roll_error], 5)
        },

        {
            "cat": "6.0 Attitude",
            "param": "Pitch Error",
            "std": 5,
            **get_stats([pitch_error], 5)
        }

    ]

    # =====================================================
    # RETURN
    # =====================================================

    return {

        "filename": os.path.basename(file_path),

        "status": status,

        "alerts": list(set(alerts)),

        "details": details,

        "events": events_log,

        "params": params_dump,

        "gps_path": gps_path,

        "flight_modes": list(set(flight_modes)),

        "summary": {

            "duration": duration_min,

            "max_altitude": max_altitude,

            "avg_current": avg_current,

            "max_current": max_current,

            "voltage_drop": voltage_drop,

            "avg_sats": avg_sats,

            "avg_hdop": avg_hdop,

            "max_vibration": max_vibe
        }
    }