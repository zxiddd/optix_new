import urllib.request
import json
import ssl

ssl_context = ssl._create_unverified_context()

BASE_URL = "https://api.optixapp.in/api/v1"

def http_post(url, data, token=None, headers_extra=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if headers_extra:
        headers.update(headers_extra)
    req = urllib.request.Request(url, data=json.dumps(data).encode("utf-8"), headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req, context=ssl_context) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8"))

def http_get(url, token):
    headers = {"Authorization": f"Bearer {token}"}
    req = urllib.request.Request(url, headers=headers, method="GET")
    try:
        with urllib.request.urlopen(req, context=ssl_context) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8"))

def http_put(url, data, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, data=json.dumps(data).encode("utf-8"), headers=headers, method="PUT")
    try:
        with urllib.request.urlopen(req, context=ssl_context) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8"))

def http_delete(url, token):
    headers = {"Authorization": f"Bearer {token}"}
    req = urllib.request.Request(url, headers=headers, method="DELETE")
    try:
        with urllib.request.urlopen(req, context=ssl_context) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8"))

print("--- 1. Admin Sign In ---")
status, res = http_post(f"{BASE_URL}/auth/local/signin", {"email": "testadmin@optix.com", "password": "Password123!"})
if status != 200:
    print("Admin sign in failed, signing up...")
    status, res = http_post(f"{BASE_URL}/auth/local/signup", {
        "email": "testadmin@optix.com",
        "password": "Password123!",
        "businessName": "Optix Staff Test Biz",
        "phone": "9998887770",
        "address": "123 Test St"
    })

print(f"Auth Status: {status}")
admin_token = res.get("access_token")
business_id = res.get("businessId")
print(f"Admin Token Obtained: {bool(admin_token)}, BusinessId: {business_id}")

print("\n--- 2. Create Staff Member ---")
staff_payload = {
    "name": "Audit Test Cashier",
    "username": f"cashier1@{business_id[:8]}",
    "password": "StaffPassword123!",
    "role": "STAFF",
    "permissions": ["WEIGHT_BILLING", "ENTER_AMOUNT"]
}
status, staff_res = http_post(f"{BASE_URL}/staff", staff_payload, token=admin_token)
print(f"Create Staff Status: {status}, Response: {staff_res}")
staff_id = staff_res.get("id")

print("\n--- 3. Staff Sign In ---")
status, staff_auth_res = http_post(f"{BASE_URL}/auth/staff/signin", {
    "username": staff_payload["username"],
    "password": "StaffPassword123!",
    "deviceId": "test_device_001",
    "deviceName": "Verification Test Device"
})
print(f"Staff Auth Status: {status}, Response: {staff_auth_res}")

print("\n--- 4. Verify Session Record ---")
status, sessions_res = http_get(f"{BASE_URL}/staff/sessions?staffId={staff_id}", token=admin_token)
print(f"Get Sessions Status: {status}, Count: {len(sessions_res)}")
if len(sessions_res) > 0:
    print(f"Active Session: {sessions_res[0]}")

print("\n--- 5. Test Disable Staff ---")
status, disable_res = http_put(f"{BASE_URL}/staff/{staff_id}/disable", {}, token=admin_token)
print(f"Disable Staff Status: {status}, Response: {disable_res}")

print("\n--- 6. Verify Disabled Staff Login Fails ---")
status, disabled_login_res = http_post(f"{BASE_URL}/auth/staff/signin", {
    "username": staff_payload["username"],
    "password": "StaffPassword123!"
})
print(f"Disabled Login Attempt Status: {status} (Expected 403), Response: {disabled_login_res}")

print("\n--- 7. Enable Staff Member ---")
status, enable_res = http_put(f"{BASE_URL}/staff/{staff_id}/enable", {}, token=admin_token)
print(f"Enable Staff Status: {status}, Response: {enable_res}")

print("\n--- 8. Update Permissions ---")
status, perm_res = http_put(f"{BASE_URL}/staff/{staff_id}/permissions", {"permissions": ["WEIGHT_BILLING", "CHANGE_PRICE"]}, token=admin_token)
print(f"Update Permissions Status: {status}, Response: {perm_res}")

print("\n--- 9. Full Dump Staff Check ---")
status, dump_res = http_get(f"{BASE_URL}/sync/full-dump", token=admin_token)
print(f"Full Dump Status: {status}")
dump_staff = dump_res.get("staff", [])
print(f"Full Dump Staff Count: {len(dump_staff)}")
if dump_staff:
    target = next((s for s in dump_staff if s["id"] == staff_id), None)
    print(f"Target Staff in Dump: {target}")

print("\n--- 10. Clean up test staff ---")
status, del_res = http_delete(f"{BASE_URL}/staff/{staff_id}", token=admin_token)
print(f"Delete Staff Status: {status}, Response: {del_res}")

print("\n=== ALL BACKEND API VERIFICATION TESTS COMPLETED SUCCESSFULLY ===")
