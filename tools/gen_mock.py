"""Generates consistent mock JSON for the Flight Info app. Usage: python3 tools/gen_mock.py app/src/main"""
import json, math, random, sys, os
from datetime import datetime, timedelta, timezone

OUT = sys.argv[1]  # app/src/main
random.seed(7)

ANCHOR = datetime(2025, 1, 1, 5, 0, tzinfo=timezone.utc)  # = 12:00 in Vietnam
ANCHOR_TS = int(ANCHOR.timestamp())

AIRPORTS = [
    # iata, icao, name, city, city_vi, country, lat, lon, utc_offset_h
    ("HAN", "VVNB", "Noi Bai International Airport", "Hanoi", "Hà Nội", "VN", 21.2212, 105.8072, 7),
    ("SGN", "VVTS", "Tan Son Nhat International Airport", "Ho Chi Minh City", "TP. Hồ Chí Minh", "VN", 10.8188, 106.6519, 7),
    ("DAD", "VVDN", "Da Nang International Airport", "Da Nang", "Đà Nẵng", "VN", 16.0439, 108.1994, 7),
    ("CXR", "VVCR", "Cam Ranh International Airport", "Nha Trang", "Nha Trang", "VN", 11.9982, 109.2194, 7),
    ("PQC", "VVPQ", "Phu Quoc International Airport", "Phu Quoc", "Phú Quốc", "VN", 10.1698, 103.9931, 7),
    ("HPH", "VVCI", "Cat Bi International Airport", "Hai Phong", "Hải Phòng", "VN", 20.8194, 106.7250, 7),
    ("VII", "VVVH", "Vinh International Airport", "Vinh", "Vinh", "VN", 18.7376, 105.6708, 7),
    ("HUI", "VVPB", "Phu Bai International Airport", "Hue", "Huế", "VN", 16.4015, 107.7026, 7),
    ("VCA", "VVCT", "Can Tho International Airport", "Can Tho", "Cần Thơ", "VN", 10.0851, 105.7119, 7),
    ("DLI", "VVDL", "Lien Khuong Airport", "Da Lat", "Đà Lạt", "VN", 11.7500, 108.3670, 7),
    ("UIH", "VVPC", "Phu Cat Airport", "Quy Nhon", "Quy Nhơn", "VN", 13.9550, 109.0420, 7),
    ("VDO", "VVVD", "Van Don International Airport", "Quang Ninh", "Quảng Ninh", "VN", 21.1178, 107.4142, 7),
    ("THD", "VVTX", "Tho Xuan Airport", "Thanh Hoa", "Thanh Hóa", "VN", 19.9017, 105.4678, 7),
    ("BMV", "VVBM", "Buon Ma Thuot Airport", "Buon Ma Thuot", "Buôn Ma Thuột", "VN", 12.6680, 108.1200, 7),
    ("PXU", "VVPK", "Pleiku Airport", "Pleiku", "Pleiku", "VN", 14.0045, 108.0170, 7),
    ("TBB", "VVTH", "Tuy Hoa Airport", "Tuy Hoa", "Tuy Hòa", "VN", 13.0496, 109.3340, 7),
    ("VCL", "VVCA", "Chu Lai Airport", "Quang Nam", "Quảng Nam", "VN", 15.4033, 108.7058, 7),
    ("VDH", "VVDH", "Dong Hoi Airport", "Dong Hoi", "Đồng Hới", "VN", 17.5150, 106.5907, 7),
    ("CAH", "VVCM", "Ca Mau Airport", "Ca Mau", "Cà Mau", "VN", 9.1777, 105.1778, 7),
    ("VCS", "VVCS", "Con Dao Airport", "Con Dao", "Côn Đảo", "VN", 8.7318, 106.6330, 7),
    ("DIN", "VVDB", "Dien Bien Phu Airport", "Dien Bien Phu", "Điện Biên Phủ", "VN", 21.3975, 103.0078, 7),
    ("VKG", "VVRG", "Rach Gia Airport", "Rach Gia", "Rạch Giá", "VN", 9.9580, 105.1324, 7),
    ("BKK", "VTBS", "Suvarnabhumi Airport", "Bangkok", "Bangkok", "TH", 13.6900, 100.7501, 7),
    ("DMK", "VTBD", "Don Mueang International Airport", "Bangkok", "Bangkok", "TH", 13.9126, 100.6068, 7),
    ("SIN", "WSSS", "Singapore Changi Airport", "Singapore", "Singapore", "SG", 1.3644, 103.9915, 8),
    ("KUL", "WMKK", "Kuala Lumpur International Airport", "Kuala Lumpur", "Kuala Lumpur", "MY", 2.7456, 101.7099, 8),
    ("PNH", "VDPP", "Phnom Penh International Airport", "Phnom Penh", "Phnôm Pênh", "KH", 11.5466, 104.8441, 7),
    ("VTE", "VLVT", "Wattay International Airport", "Vientiane", "Viêng Chăn", "LA", 17.9883, 102.5633, 7),
    ("HKG", "VHHH", "Hong Kong International Airport", "Hong Kong", "Hồng Kông", "HK", 22.3080, 113.9185, 8),
    ("TPE", "RCTP", "Taoyuan International Airport", "Taipei", "Đài Bắc", "TW", 25.0777, 121.2328, 8),
    ("CAN", "ZGGG", "Guangzhou Baiyun International Airport", "Guangzhou", "Quảng Châu", "CN", 23.3924, 113.2988, 8),
    ("PVG", "ZSPD", "Shanghai Pudong International Airport", "Shanghai", "Thượng Hải", "CN", 31.1443, 121.8083, 8),
    ("ICN", "RKSI", "Incheon International Airport", "Seoul", "Seoul", "KR", 37.4602, 126.4407, 9),
    ("NRT", "RJAA", "Narita International Airport", "Tokyo", "Tokyo", "JP", 35.7647, 140.3864, 9),
    ("HND", "RJTT", "Haneda Airport", "Tokyo", "Tokyo", "JP", 35.5523, 139.7798, 9),
    ("KIX", "RJBB", "Kansai International Airport", "Osaka", "Osaka", "JP", 34.4347, 135.2440, 9),
    ("MNL", "RPLL", "Ninoy Aquino International Airport", "Manila", "Manila", "PH", 14.5086, 121.0194, 8),
    ("CGK", "WIII", "Soekarno-Hatta International Airport", "Jakarta", "Jakarta", "ID", -6.1256, 106.6559, 7),
    ("DPS", "WADD", "Ngurah Rai International Airport", "Bali", "Bali", "ID", -8.7482, 115.1672, 8),
    ("SYD", "YSSY", "Sydney Kingsford Smith Airport", "Sydney", "Sydney", "AU", -33.9399, 151.1753, 11),
    ("DXB", "OMDB", "Dubai International Airport", "Dubai", "Dubai", "AE", 25.2528, 55.3644, 4),
    ("DOH", "OTHH", "Hamad International Airport", "Doha", "Doha", "QA", 25.2731, 51.6081, 3),
    ("CDG", "LFPG", "Paris Charles de Gaulle Airport", "Paris", "Paris", "FR", 49.0097, 2.5479, 1),
    ("FRA", "EDDF", "Frankfurt Airport", "Frankfurt", "Frankfurt", "DE", 50.0379, 8.5622, 1),
    ("LHR", "EGLL", "London Heathrow Airport", "London", "Luân Đôn", "GB", 51.4700, -0.4543, 0),
]
AP = {a[0]: a for a in AIRPORTS}

AIRLINE_ICAO = {"VN": "HVN", "VJ": "VJC", "QH": "BAV", "BL": "PIC", "VU": "VAG", "TG": "THA", "SQ": "SIA"}

# flight, dep, arr, dep offset from "now" (min), duration (min), delay (min), cancelled, aircraft, registration
FLIGHTS = [
    ("VN215", "HAN", "SGN", -60, 130, 0, False, "A321", "VN-A615"),
    ("VJ123", "HAN", "SGN", 20, 125, 0, False, "A321", "VN-A651"),
    ("QH201", "HAN", "SGN", 45, 130, 35, False, "A321", "VN-A588"),
    ("VN161", "HAN", "DAD", 70, 80, 0, False, "A321", "VN-A397"),
    ("VJ501", "HAN", "DAD", -150, 80, 0, False, "A320", "VN-A675"),
    ("VJ773", "HAN", "CXR", 95, 110, 0, False, "A321", "VN-A535"),
    ("VN1233", "HAN", "PQC", 130, 135, 20, False, "A321", "VN-A613"),
    ("BL6020", "HAN", "SGN", -20, 130, 0, True, "A320", "VN-A566"),
    ("VN310", "HAN", "NRT", 140, 330, 0, False, "A359", "VN-A891"),
    ("VN416", "HAN", "ICN", -90, 270, 0, False, "A359", "VN-A886"),
    ("TG561", "HAN", "BKK", 55, 110, 0, False, "A320", "HS-TXA"),
    ("SQ193", "HAN", "SIN", 180, 200, 0, False, "B738", "9V-MGA"),
    ("VN220", "SGN", "HAN", -100, 130, 0, False, "A321", "VN-A622"),
    ("VJ124", "SGN", "HAN", -40, 125, 0, False, "A321", "VN-A652"),
    ("QH202", "SGN", "HAN", -200, 130, 10, False, "A321", "VN-A589"),
    ("VN162", "DAD", "HAN", 10, 80, 0, False, "A321", "VN-A398"),
    ("VJ502", "DAD", "HAN", 60, 80, 25, False, "A320", "VN-A676"),
    ("VN1234", "PQC", "HAN", -170, 135, 0, False, "A321", "VN-A614"),
    ("VN417", "ICN", "HAN", -240, 280, 0, False, "A359", "VN-A887"),
    ("TG560", "BKK", "HAN", 100, 105, 0, False, "A320", "HS-TXB"),
    ("VU751", "SGN", "HAN", 150, 130, 0, False, "A321", "VN-A221"),
    ("VN311", "NRT", "HAN", -500, 360, 0, False, "A359", "VN-A892"),
    ("SQ192", "SIN", "HAN", -90, 205, 0, False, "B738", "9V-MGB"),
    ("VN124", "SGN", "DAD", 30, 85, 0, False, "A321", "VN-A399"),
    ("VJ621", "SGN", "DAD", -30, 85, 0, False, "A320", "VN-A677"),
    ("VJ322", "SGN", "CXR", 50, 60, 0, False, "A320", "VN-A678"),
    ("VN1825", "SGN", "PQC", -70, 60, 0, False, "A321", "VN-A616"),
    ("VJ282", "HPH", "SGN", 40, 125, 15, False, "A321", "VN-A653"),
    ("VN1270", "SGN", "VII", -20, 115, 0, False, "A321", "VN-A617"),
    ("VN125", "DAD", "SGN", -75, 85, 0, False, "A321", "VN-A400"),
]


def fmt(dt):
    return dt.strftime("%Y-%m-%d %H:%M")


def terminal(iata, other):
    domestic = AP[iata][5] == "VN" and AP[other][5] == "VN"
    if iata in ("HAN", "SGN"):
        return "1" if domestic else "2"
    return "1" if iata in ("DAD", "CXR", "PQC") else None


def put_time(o, prefix, dt_utc, tz):
    o[prefix] = fmt(dt_utc + timedelta(hours=tz))
    o[prefix + "_utc"] = fmt(dt_utc)
    o[prefix + "_ts"] = int(dt_utc.timestamp())


flights = []
for code, dep, arr, off, dur, delay, cancelled, ac, reg in FLIGHTS:
    airline = code[:2] if not code[:2].isdigit() else code[:2]
    dep_sched = ANCHOR + timedelta(minutes=off)
    dep_est = dep_sched + timedelta(minutes=delay)
    arr_sched = dep_sched + timedelta(minutes=dur)
    arr_est = dep_est + timedelta(minutes=dur)
    dtz, atz = AP[dep][8], AP[arr][8]
    if cancelled:
        status = "cancelled"
    elif ANCHOR >= arr_est:
        status = "landed"
    elif ANCHOR >= dep_est:
        status = "active"
    else:
        status = "scheduled"

    o = {
        "airline_iata": airline, "airline_icao": AIRLINE_ICAO[airline],
        "flight_iata": code, "flight_icao": AIRLINE_ICAO[airline] + code[2:], "flight_number": code[2:],
        "dep_iata": dep, "dep_icao": AP[dep][1], "dep_terminal": terminal(dep, arr),
        "dep_gate": str(random.randint(1, 30)) if dep in ("HAN", "SGN", "DAD") else None,
    }
    put_time(o, "dep_time", dep_sched, dtz)
    if not cancelled:
        put_time(o, "dep_estimated", dep_est, dtz)
        if status in ("active", "landed"):
            put_time(o, "dep_actual", dep_est, dtz)
    o.update({"arr_iata": arr, "arr_icao": AP[arr][1], "arr_terminal": terminal(arr, dep),
              "arr_gate": str(random.randint(1, 20)) if arr in ("HAN", "SGN") else None,
              "arr_baggage": str(random.randint(1, 9)) if not cancelled else None})
    put_time(o, "arr_time", arr_sched, atz)
    if not cancelled:
        put_time(o, "arr_estimated", arr_est, atz)
        if status == "landed":
            put_time(o, "arr_actual", arr_est, atz)
    o.update({"status": status, "duration": dur, "dep_delayed": delay or None, "arr_delayed": delay or None,
              "aircraft_icao": ac, "reg_number": reg})
    flights.append({k: v for k, v in o.items() if v is not None})

os.makedirs(os.path.join(OUT, "res/raw"), exist_ok=True)
with open(os.path.join(OUT, "res/raw/mock_flights.json"), "w") as f:
    json.dump({"anchor_ts": ANCHOR_TS, "flights": flights}, f, indent=1, ensure_ascii=False)


# --- OpenSky mock: airborne flights placed along their route + some overflights / ground traffic ---
def bearing(lat1, lon1, lat2, lon2):
    p1, p2, dl = math.radians(lat1), math.radians(lat2), math.radians(lon2 - lon1)
    x = math.sin(dl) * math.cos(p2)
    y = math.cos(p1) * math.sin(p2) - math.sin(p1) * math.cos(p2) * math.cos(dl)
    return (math.degrees(math.atan2(x, y)) + 360) % 360


def in_box(lat, lon):
    return 8 <= lat <= 24 and 102 <= lon <= 110


COUNTRY = {"VN": "Viet Nam", "TG": "Thailand", "SQ": "Singapore", "QH": "Viet Nam", "VJ": "Viet Nam",
           "BL": "Viet Nam", "VU": "Viet Nam"}
states = []
hexid = 0x888000
for fl in flights:
    if fl["status"] != "active":
        continue
    a, b = AP[fl["dep_iata"]], AP[fl["arr_iata"]]
    frac = (ANCHOR_TS - fl["dep_actual_ts"]) / (fl["arr_estimated_ts"] - fl["dep_actual_ts"])
    lat = a[6] + (b[6] - a[6]) * frac
    lon = a[7] + (b[7] - a[7]) * frac
    if not in_box(lat, lon):
        # long-haul: show it where it crosses into the box, close to Vietnam
        candidates = [i / 200 for i in range(201)
                      if in_box(a[6] + (b[6] - a[6]) * i / 200, a[7] + (b[7] - a[7]) * i / 200)]
        t = min(candidates, key=lambda c: abs(c - frac))
        lat, lon = a[6] + (b[6] - a[6]) * t, a[7] + (b[7] - a[7]) * t
    hexid += 0x11
    states.append([format(hexid, "06x"), fl["flight_icao"].ljust(8), COUNTRY[fl["airline_iata"]],
                   ANCHOR_TS - 3, ANCHOR_TS - 1, round(lon, 4), round(lat, 4),
                   random.choice([10058.4, 10363.2, 10668.0, 10972.8, 11277.6]), False,
                   round(random.uniform(215, 250), 1), round(bearing(a[6], a[7], b[6], b[7]), 1), 0.0,
                   None, None, None, False, 0])

EXTRA = [  # overflights and other traffic: callsign, country, lat, lon, alt, track, velocity, vrate, on_ground
    ("CPA761  ", "China", 12.6, 109.7, 11582.4, 210, 245, 0, False),
    ("THA615  ", "Thailand", 14.2, 103.4, 10972.8, 75, 238, 0, False),
    ("SIA176  ", "Singapore", 9.4, 107.9, 11277.6, 32, 241, 0, False),
    ("KAL462  ", "Republic of Korea", 19.8, 108.9, 11887.2, 195, 247, 0, False),
    ("AXM521  ", "Malaysia", 8.9, 104.6, 10363.2, 350, 232, 0, False),
    ("QTR970  ", "Qatar", 17.2, 103.1, 11887.2, 70, 252, 0, False),
    ("HVN1543 ", "Viet Nam", 15.7, 108.5, 3048.0, 160, 160, -7.5, False),
    ("VJC1342 ", "Viet Nam", 20.9, 105.6, 2133.6, 120, 140, 9.0, False),
    ("BAV2061 ", "Viet Nam", 11.3, 106.9, 4572.0, 15, 180, 11.0, False),
    ("HVN7820 ", "Viet Nam", 21.2178, 105.8020, 0.0, 110, 0, 0, True),
    ("VJC199  ", "Viet Nam", 10.8150, 106.6570, 0.0, 250, 0, 0, True),
    ("EVA392  ", "Taiwan", 22.6, 109.4, 11582.4, 230, 240, 0, False),
    ("CES757  ", "China", 23.1, 106.2, 10668.0, 190, 236, 0, False),
    ("PIC6033 ", "Viet Nam", 13.4, 108.4, 9753.6, 175, 225, 0, False),
]
for cs, country, lat, lon, alt, trk, vel, vr, ground in EXTRA:
    hexid += 0x11
    states.append([format(hexid, "06x"), cs, country, ANCHOR_TS - 3, ANCHOR_TS - 1, lon, lat,
                   None if ground else alt, ground, float(vel), float(trk), float(vr),
                   None, None, None, False, 0])

with open(os.path.join(OUT, "res/raw/mock_opensky.json"), "w") as f:
    json.dump({"time": ANCHOR_TS, "states": states}, f, indent=1)

# --- METAR mock ---
METAR = [  # icao, temp, dewp, wdir, wspd, visib, cat
    ("VVNB", 24, 18, 120, 6, "6+", "VFR"), ("VVTS", 32, 24, 200, 9, "6+", "VFR"),
    ("VVDN", 27, 22, 90, 7, "6+", "VFR"), ("VVCR", 29, 23, 60, 11, "6+", "VFR"),
    ("VVPQ", 30, 25, 250, 8, "5", "MVFR"), ("VVCI", 23, 19, "VRB", 2, "4", "MVFR"),
    ("VVVH", 25, 20, 140, 5, "6+", "VFR"), ("VVPB", 22, 20, 320, 4, "3", "MVFR"),
    ("VVCT", 31, 24, 190, 6, "6+", "VFR"), ("VVDL", 19, 14, 70, 8, "6+", "VFR"),
    ("VTBS", 33, 24, 180, 10, "6+", "VFR"), ("WSSS", 30, 25, 340, 7, "6+", "VFR"),
    ("RKSI", 3, -4, 320, 14, "6+", "VFR"), ("RJAA", 8, 1, 10, 12, "6+", "VFR"),
    ("VHHH", 21, 15, 70, 13, "6+", "VFR"),
]
metars = []
for icao, t, d, wdir, wspd, vis, cat in METAR:
    a = next(x for x in AIRPORTS if x[1] == icao)
    wd = "VRB" if wdir == "VRB" else f"{wdir:03d}"
    vis_m = "9999" if vis == "6+" else str(int(vis) * 1600)
    temp_s = lambda v: f"M{abs(v):02d}" if v < 0 else f"{v:02d}"
    raw = f"METAR {icao} 010430Z {wd}{wspd:02d}KT {vis_m} FEW020 SCT100 {temp_s(t)}/{temp_s(d)} Q1012 NOSIG"
    metars.append({"icaoId": icao, "name": a[2] + ", " + a[3], "obsTime": ANCHOR_TS - 1800,
                   "temp": t, "dewp": d, "wdir": wdir, "wspd": wspd, "visib": vis, "altim": 1012,
                   "fltCat": cat, "rawOb": raw})
with open(os.path.join(OUT, "res/raw/mock_metar.json"), "w") as f:
    json.dump(metars, f, indent=1)

# --- airports.json (assets) ---
os.makedirs(os.path.join(OUT, "assets"), exist_ok=True)
with open(os.path.join(OUT, "assets/airports.json"), "w") as f:
    json.dump([{"iata": a[0], "icao": a[1], "name": a[2], "city": a[3], "city_vi": a[4], "country": a[5],
                "lat": a[6], "lon": a[7]} for a in AIRPORTS], f, indent=1, ensure_ascii=False)

# Summary for sanity checking
from collections import Counter
print("flights:", len(flights), Counter(f["status"] for f in flights))
print("HAN dep:", sum(f["dep_iata"] == "HAN" for f in flights), "HAN arr:", sum(f["arr_iata"] == "HAN" for f in flights))
print("SGN dep:", sum(f["dep_iata"] == "SGN" for f in flights), "SGN arr:", sum(f["arr_iata"] == "SGN" for f in flights))
print("aircraft:", len(states), "airports:", len(AIRPORTS), "metars:", len(metars))
for s in states[:16]:
    print(s[1], s[6], s[5], s[10])
