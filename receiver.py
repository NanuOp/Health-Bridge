#!/usr/bin/env python3
"""
HealthBridge Receiver — Runs on Ubuntu PC (192.168.0.104:8765)
Receives health data from the HealthBridge Android app and displays it.

Usage:
    python3 receiver.py
"""

from http.server import HTTPServer, BaseHTTPRequestHandler
import json
import datetime
import os
import sqlite3

DATA_LOG_FILE = "health_data_log.json"
DB_FILE = "healthbridge.db"
PORT = 8765

def init_db():
    conn = sqlite3.connect(DB_FILE)
    c = conn.cursor()
    c.execute('''
        CREATE TABLE IF NOT EXISTS recovery_scores (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            finalScore INTEGER,
            hrvScore INTEGER,
            restingHRScore INTEGER,
            sleepScore INTEGER,
            spO2Score INTEGER,
            zone TEXT,
            insight TEXT,
            timestamp TEXT
        )
    ''')
    conn.commit()
    conn.close()

class HealthHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/ping":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({"status": "ok", "server": "HealthBridge Receiver"}).encode())
        elif self.path == "/dashboard":
            self.send_response(200)
            self.send_header("Content-Type", "text/html")
            self.end_headers()
            
            conn = sqlite3.connect(DB_FILE)
            c = conn.cursor()
            c.execute('SELECT finalScore, zone, insight, timestamp FROM recovery_scores ORDER BY id DESC LIMIT 1')
            row = c.fetchone()
            conn.close()
            
            if row:
                score, zone, insight, timestamp = row
                # Default dark theme colors
                color = "#FF4444"
                if score >= 85: color = "#00FF88"
                elif score >= 70: color = "#FFD700"
                elif score >= 50: color = "#FFA500"
                
                html = f"""
                <html>
                <head>
                    <title>HealthBridge Dashboard</title>
                    <style>
                        body {{ background-color: #0A0A0F; color: white; font-family: sans-serif; padding: 40px; text-align: center; }}
                        .card {{ background-color: {color}33; border: 2px solid {color}; border-radius: 20px; padding: 40px; max-width: 600px; margin: 0 auto; }}
                        .score {{ font-size: 100px; font-weight: bold; color: {color}; margin: 0; }}
                        .zone {{ font-size: 24px; color: {color}; text-transform: uppercase; letter-spacing: 2px; margin-bottom: 20px; }}
                        .insight {{ font-size: 18px; line-height: 1.5; color: #DDDDDD; text-align: left; background: rgba(0,0,0,0.5); padding: 20px; border-radius: 10px; }}
                        .time {{ color: #888; font-size: 12px; margin-top: 20px; }}
                    </style>
                </head>
                <body>
                    <h1>HealthBridge Server</h1>
                    <div class="card">
                        <div class="zone">{zone}</div>
                        <h2 class="score">{score}</h2>
                        <div class="insight">{insight.replace('. ', '.<br><br>')}</div>
                        <div class="time">Last updated: {timestamp}</div>
                    </div>
                </body>
                </html>
                """
            else:
                html = "<html><body style='background:#0A0A0F;color:white;text-align:center;padding:50px;'><h1>No recovery data yet</h1></body></html>"
                
            self.wfile.write(html.encode("utf-8"))
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        if self.path == "/health":
            content_length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(content_length)

            try:
                data = json.loads(body.decode("utf-8"))
                self.process_health_data(data)

                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.end_headers()
                self.wfile.write(json.dumps({"status": "received", "time": str(datetime.datetime.now())}).encode())
            except json.JSONDecodeError as e:
                self.send_response(400)
                self.end_headers()
                self.wfile.write(json.dumps({"error": str(e)}).encode())
        elif self.path == "/recovery":
            content_length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(content_length)

            try:
                data = json.loads(body.decode("utf-8"))
                self.process_recovery_data(data)

                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.end_headers()
                self.wfile.write(json.dumps({"status": "received", "time": str(datetime.datetime.now())}).encode())
            except json.JSONDecodeError as e:
                self.send_response(400)
                self.end_headers()
                self.wfile.write(json.dumps({"error": str(e)}).encode())
        else:
            self.send_response(404)
            self.end_headers()

    def process_recovery_data(self, data):
        now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        print("\n" + "=" * 60)
        print(f"🌟 Recovery Data Received at {now}")
        print(f"   Score: {data.get('finalScore', 0)}/100 [{data.get('zone', 'Unknown')}]")
        print(f"   HRV: {data.get('hrvScore', 0)}, RHR: {data.get('restingHRScore', 0)}, Sleep: {data.get('sleepScore', 0)}, SpO2: {data.get('spO2Score', 0)}")
        print(f"   Insight: {data.get('insight', 'None')}")
        print("=" * 60)

        conn = sqlite3.connect(DB_FILE)
        c = conn.cursor()
        c.execute('''
            INSERT INTO recovery_scores (finalScore, hrvScore, restingHRScore, sleepScore, spO2Score, zone, insight, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ''', (
            data.get('finalScore', 0),
            data.get('hrvScore', 0),
            data.get('restingHRScore', 0),
            data.get('sleepScore', 0),
            data.get('spO2Score', 0),
            data.get('zone', 'Unknown'),
            data.get('insight', ''),
            data.get('timestamp', now)
        ))
        conn.commit()
        conn.close()
        print(f"💾 Saved Recovery Score to SQLite DB ({DB_FILE})")

    def process_health_data(self, data):
        now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        print("\n" + "=" * 60)
        print(f"📱 Health Data Received at {now}")
        print(f"   Device: {data.get('device', 'Unknown')}")
        print(f"   MAC: {data.get('mac', 'Unknown')}")
        print(f"   Sync Time: {data.get('syncTime', 'Unknown')}")
        print("=" * 60)

        # Battery
        battery = data.get("battery")
        if battery is not None:
            print(f"\n🔋 Battery: {battery}%")

        # Real-time health
        rt = data.get("realTimeHealth")
        if rt:
            print("\n💓 Real-Time Health Data:")
            print(f"   ❤️  Heart Rate:    {rt.get('heartRate', 0)} bpm")
            print(f"   🫁  SpO2:          {rt.get('oxygen', 0)}%")
            print(f"   🩸  Blood Pressure: {rt.get('systolicBP', 0)}/{rt.get('diastolicBP', 0)} mmHg")
            print(f"   🌡️  Body Temp:     {rt.get('bodyTemp', 0):.1f}°C")
            print(f"   😮‍💨  Respiratory:   {rt.get('respiratoryRate', 0)} breaths/min")
            print(f"   🧠  Stress Level:  {rt.get('stressLevel', 0)}")

        # Today total
        today = data.get("todayTotal")
        if today:
            print("\n📊 Today's Summary:")
            print(f"   👟 Steps:    {today.get('totalSteps', 0)}")
            print(f"   📏 Distance: {today.get('totalDistanceM', 0)}m")
            print(f"   🔥 Calories: {today.get('totalCalories', 0)} kcal")
            print(f"   ❤️  HR:      {today.get('currentHR', 0)} bpm")

        # Steps history
        steps = data.get("steps")
        if steps:
            print(f"\n👟 Step Records ({len(steps)} entries):")
            for s in steps[:5]:  # Show first 5
                print(f"   {s.get('steps', 0)} steps, {s.get('distanceKm', 0):.2f}km, "
                      f"{s.get('caloriesKcal', 0):.1f}kcal, {s.get('durationSec', 0)}s")

        # Heart rate history
        hr_list = data.get("heartRate")
        if hr_list:
            print(f"\n❤️ Heart Rate Records ({len(hr_list)} entries):")
            for h in hr_list[:5]:
                print(f"   {h.get('bpm', 0)} bpm")

        # Sleep
        sleep = data.get("sleep")
        if sleep:
            print(f"\n😴 Sleep Records ({len(sleep)} entries):")
            for s in sleep[:5]:
                print(f"   {s.get('stateName', 'Unknown')}: {s.get('durationMinutes', 0)} min")

        print("\n" + "-" * 60)

        # Save to file
        self.save_data(data)

    def save_data(self, data):
        entries = []
        if os.path.exists(DATA_LOG_FILE):
            try:
                with open(DATA_LOG_FILE, "r") as f:
                    entries = json.load(f)
            except (json.JSONDecodeError, IOError):
                entries = []

        data["_received_at"] = str(datetime.datetime.now())
        entries.append(data)

        with open(DATA_LOG_FILE, "w") as f:
            json.dump(entries, f, indent=2)

        print(f"💾 Saved to {DATA_LOG_FILE} ({len(entries)} total entries)")

    def log_message(self, format, *args):
        pass  # Suppress default HTTP logs

if __name__ == "__main__":
    init_db()
    server = HTTPServer(("0.0.0.0", PORT), HealthHandler)
    print(f"🏥 HealthBridge Receiver started on port {PORT}")
    print(f"   Listening on http://0.0.0.0:{PORT}")
    print(f"   Waiting for data from HealthBridge app...")
    print(f"   Press Ctrl+C to stop\n")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n\n🛑 Server stopped.")
        server.server_close()
