import requests
import json

API_BASE = "https://api.optixapp.in/api/v1"

print("--- MILESTONE 3 VERIFICATION SCRIPT ---")

# 1. Login owner
login_res = requests.post(f"{API_BASE}/auth/local/signup", json={
    "email": "reset_owner_m3@optix.com",
    "password": "Password123!",
    "businessName": "Optix Milestone 3 Reset POS"
})

if login_res.status_code != 200 and login_res.status_code != 201:
    login_res = requests.post(f"{API_BASE}/auth/local/signin", json={
        "email": "reset_owner_m3@optix.com",
        "password": "Password123!"
    })

print(f"Auth status: {login_res.status_code}")
data = login_res.json()
token = data.get("access_token") or data.get("accessToken") or data.get("token")
print(f"Auth token retrieved: {bool(token)}")

headers = {"Authorization": f"Bearer {token}"}

# 2. Trigger first reset for date "2026-08-05"
reset_payload = {"targetBusinessDate": "2026-08-05"}
print(f"Triggering 1st reset: {reset_payload}")
r1 = requests.post(f"{API_BASE}/business/reset", headers=headers, json=reset_payload)
print("1st Reset Status:", r1.status_code)
print("1st Reset Response:", r1.text)

# 3. Trigger SECOND reset for same date "2026-08-05" (Idempotency Guard Test)
print(f"Triggering 2nd reset (IDEMPOTENCY TEST): {reset_payload}")
r2 = requests.post(f"{API_BASE}/business/reset", headers=headers, json=reset_payload)
print("2nd Reset Status:", r2.status_code)
print("2nd Reset Response:", r2.text)

res2_json = r2.json()

if res2_json.get("alreadyReset") is True:
    print("\n[PASS] IDEMPOTENCY VERIFIED: 2nd reset attempt was caught by Exactly-Once guard (alreadyReset = true)!")
else:
    print("\n[FAIL] IDEMPOTENCY FAILURE: 2nd reset executed again!")

# 4. Verify profile lastResetBusinessDate
prof_res = requests.get(f"{API_BASE}/business/profile", headers=headers)
p_json = prof_res.json()
last_reset = p_json.get("settings", {}).get("lastResetBusinessDate")
print(f"Profile lastResetBusinessDate: {last_reset}")

if last_reset == "2026-08-05":
    print("[PASS] PROFILE VERIFIED: lastResetBusinessDate correctly persisted as 2026-08-05 in PostgreSQL DB!")
else:
    print(f"[FAIL] PROFILE MISMATCH: Expected 2026-08-05, got {last_reset}")
