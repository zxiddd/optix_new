import requests
import json
import time

API_BASE = "https://api.optixapp.in/api/v1"

print("--- MILESTONE 1 VERIFICATION SCRIPT ---")

# 1. Login or Signup owner
login_res = requests.post(f"{API_BASE}/auth/local/signin", json={
    "email": "timings_owner@optix.com",
    "password": "Password123!"
})

if login_res.status_code != 200 and login_res.status_code != 201:
    print(f"Signin failed ({login_res.status_code}), attempting signup...")
    signup_res = requests.post(f"{API_BASE}/auth/local/signup", json={
        "email": "timings_owner@optix.com",
        "password": "Password123!",
        "businessName": "Optix Timings POS"
    })
    print(f"Signup status: {signup_res.status_code} {signup_res.text}")
    login_res = signup_res

print(f"Login response: {login_res.status_code}")
data = login_res.json()
token = data.get("access_token") or data.get("accessToken") or data.get("token")
print(f"Auth token retrieved: {bool(token)}")

headers = {"Authorization": f"Bearer {token}"}

# 2. GET /business/profile
profile_res = requests.get(f"{API_BASE}/business/profile", headers=headers)
print(f"GET /business/profile status: {profile_res.status_code}")
p_json = profile_res.json()
settings = p_json.get("settings", {})
print("Current Settings:", json.dumps(settings, indent=2))

# 3. POST /business/profile to update timings
update_payload = {
    "openingTime": "09:30",
    "closingTime": "23:00",
    "timezone": "Asia/Riyadh"
}
print(f"Updating timings: {update_payload}")
update_res = requests.post(f"{API_BASE}/business/profile", headers=headers, json=update_payload)
print(f"POST /business/profile status: {update_res.status_code}")

# 4. Verify updated profile via GET
verify_res = requests.get(f"{API_BASE}/business/profile", headers=headers)
v_json = verify_res.json()
v_settings = v_json.get("settings", {})
print("Updated Settings:", json.dumps(v_settings, indent=2))

op_time = v_settings.get("openingTime")
cl_time = v_settings.get("closingTime")
tz = v_settings.get("timezone")

if op_time == "09:30" and cl_time == "23:00" and tz == "Asia/Riyadh":
    print("VERIFICATION SUCCESS: OpeningTime, ClosingTime, and Timezone match updated values!")
else:
    print(f"VERIFICATION WARNING: Mismatch! op={op_time}, cl={cl_time}, tz={tz}")
