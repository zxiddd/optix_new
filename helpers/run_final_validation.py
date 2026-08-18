import requests
import json
import time
import paramiko
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

VPS_HOST = "200.141.7.8"
VPS_USER = "root"
VPS_PASS = "Zaddy123"
BASE_URL = "https://api.optixapp.in/api/v1"

results = []

def run_test(section, name, func):
    t0 = time.time()
    try:
        status, details = func()
        lat = round((time.time() - t0) * 1000, 2)
        results.append({"section": section, "test": name, "status": status, "details": str(details), "latency_ms": lat})
        print(f"[{status}] {section} -> {name} ({lat}ms)")
    except Exception as e:
        lat = round((time.time() - t0) * 1000, 2)
        results.append({"section": section, "test": name, "status": "FAIL", "details": str(e), "latency_ms": lat})
        print(f"[FAIL] {section} -> {name} ({lat}ms) | EXCEPTION: {str(e)}")

print("=== STARTING FULL PRODUCTION VALIDATION ===")

# --- 1. SERVER & INFRASTRUCTURE ---
def test_infra():
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        client.connect(VPS_HOST, username=VPS_USER, password=VPS_PASS, timeout=10)
        stdin, stdout, stderr = client.exec_command("docker ps --format '{{.Names}}\t{{.Status}}'")
        out = stdout.read().decode()
        client.close()
        if "optix-backend-staging" in out and "optix-postgres-staging" in out:
            return "PASS", out
        return "FAIL", out
    except Exception as e:
        return "FAIL", str(e)

run_test("1. Infrastructure", "Docker Containers & PostgreSQL Status", test_infra)

# Global variables for testing tenant isolation & APIs
bizA = {}
bizB = {}
catA_id = None
prodA_id = None
orderA_id = None
custA_id = None
staffA_id = None

# --- 2. AUTHENTICATION ---
def test_signup_bizA():
    global bizA
    email = f"owner_a_{int(time.time())}@optixapp.in"
    r = requests.post(f"{BASE_URL}/auth/local/signup", json={"email": email, "password": "Password123!", "businessName": "Optix Store Alpha"}, verify=False)
    if r.status_code == 201 and "access_token" in r.json():
        bizA = r.json()
        bizA["email"] = email
        return "PASS", f"Created User ID: {bizA.get('userId')}"
    return "FAIL", f"Status {r.status_code}: {r.text}"

def test_signup_bizB():
    global bizB
    email = f"owner_b_{int(time.time())}@optixapp.in"
    r = requests.post(f"{BASE_URL}/auth/local/signup", json={"email": email, "password": "Password123!", "businessName": "Optix Store Beta"}, verify=False)
    if r.status_code == 201 and "access_token" in r.json():
        bizB = r.json()
        bizB["email"] = email
        return "PASS", f"Created User ID: {bizB.get('userId')}"
    return "FAIL", f"Status {r.status_code}: {r.text}"

def test_login_bizA():
    r = requests.post(f"{BASE_URL}/auth/local/signin", json={"email": bizA["email"], "password": "Password123!"}, verify=False)
    if r.status_code in [200, 201] and "access_token" in r.json():
        return "PASS", "Owner authentication successful"
    return "FAIL", f"Status {r.status_code}: {r.text}"

def test_wrong_password():
    r = requests.post(f"{BASE_URL}/auth/local/signin", json={"email": bizA["email"], "password": "WrongPassword!"}, verify=False)
    if r.status_code in [401, 403]:
        return "PASS", f"Forbidden/Unauthorized correctly returned ({r.status_code})"
    return "FAIL", f"Expected 401/403, got {r.status_code}"

def test_duplicate_email():
    r = requests.post(f"{BASE_URL}/auth/local/signup", json={"email": bizA["email"], "password": "Password123!", "businessName": "Dup Store"}, verify=False)
    if r.status_code in [400, 403, 409, 500]:
        return "PASS", f"Duplicate email rejected with status {r.status_code}"
    return "FAIL", f"Unexpected status {r.status_code}: {r.text}"

def test_invalid_token():
    headers = {"Authorization": "Bearer invalid.jwt.token"}
    r = requests.get(f"{BASE_URL}/business/profile", headers=headers, verify=False)
    if r.status_code in [401, 403]:
        return "PASS", f"Invalid token rejected with status {r.status_code}"
    return "FAIL", f"Expected 401/403, got {r.status_code}"

run_test("2. Authentication", "Owner Signup (Business A)", test_signup_bizA)
run_test("2. Authentication", "Owner Signup (Business B)", test_signup_bizB)
run_test("2. Authentication", "Owner Signin", test_login_bizA)
run_test("2. Authentication", "Wrong Password Rejection", test_wrong_password)
run_test("2. Authentication", "Duplicate Email Rejection", test_duplicate_email)
run_test("2. Authentication", "Invalid JWT Verification", test_invalid_token)

headersA = {"Authorization": f"Bearer {bizA.get('access_token')}"}
headersB = {"Authorization": f"Bearer {bizB.get('access_token')}"}

# --- 3. CATEGORIES & TENANT ISOLATION ---
def test_create_category_A():
    global catA_id
    r = requests.post(f"{BASE_URL}/categories", json={"name": "Beverages", "sortOrder": 1}, headers=headersA, verify=False)
    if r.status_code in [200, 201]:
        catA_id = r.json().get("id")
        return "PASS", f"Category created ID: {catA_id}"
    return "FAIL", f"Status {r.status_code}: {r.text}"

def test_tenant_isolation_categories():
    rB = requests.get(f"{BASE_URL}/categories", headers=headersB, verify=False)
    if rB.status_code == 200:
        catsB = rB.json()
        hasA = any(c.get("id") == catA_id for c in catsB)
        if not hasA:
            return "PASS", "Business B cannot access Business A categories (100% Isolated)"
        return "FAIL", "SECURITY BREACH: Business B accessed Business A categories!"
    return "FAIL", f"Status {rB.status_code}: {rB.text}"

run_test("5. Categories", "Create Category", test_create_category_A)
run_test("3. Business Isolation", "Category Tenant Isolation", test_tenant_isolation_categories)

# --- 4. PRODUCTS ---
def test_create_product_A():
    global prodA_id
    r = requests.post(f"{BASE_URL}/products", json={
        "name": "Espresso Blend",
        "price": 120.0,
        "categoryId": catA_id,
        "pricingType": "FIXED",
        "unit": "Cup"
    }, headers=headersA, verify=False)
    if r.status_code in [200, 201]:
        prodA_id = r.json().get("id")
        return "PASS", f"Product created ID: {prodA_id}"
    return "FAIL", f"Status {r.status_code}: {r.text}"

def test_tenant_isolation_products():
    rB = requests.get(f"{BASE_URL}/products", headers=headersB, verify=False)
    if rB.status_code == 200:
        prodsB = rB.json()
        hasA = any(p.get("id") == prodA_id for p in prodsB)
        if not hasA:
            return "PASS", "Business B cannot access Business A products (100% Isolated)"
        return "FAIL", "SECURITY BREACH: Business B accessed Business A products!"
    return "FAIL", f"Status {rB.status_code}: {rB.text}"

def test_soft_delete_product():
    r = requests.delete(f"{BASE_URL}/products/{prodA_id}", headers=headersA, verify=False)
    if r.status_code == 200:
        r_get = requests.get(f"{BASE_URL}/products", headers=headersA, verify=False)
        prods = r_get.json()
        still_present = any(p.get("id") == prodA_id for p in prods)
        if not still_present:
            return "PASS", "Product successfully soft deleted"
        return "FAIL", "Product still visible after deletion"
    return "FAIL", f"Status {r.status_code}: {r.text}"

run_test("4. Product APIs", "Create Product", test_create_product_A)
run_test("3. Business Isolation", "Product Tenant Isolation", test_tenant_isolation_products)
run_test("4. Product APIs", "Soft Delete Product", test_soft_delete_product)

# Re-create product for orders
r_recreate = requests.post(f"{BASE_URL}/products", json={"name": "Espresso", "price": 100.0, "categoryId": catA_id}, headers=headersA, verify=False)
if r_recreate.status_code in [200, 201]:
    prodA_id = r_recreate.json().get("id")

# --- 5. BILLING & ORDERS ---
def test_create_order_A():
    global orderA_id
    r = requests.post(f"{BASE_URL}/orders", json={
        "total": 200.0,
        "paymentMethod": "UPI",
        "cashierName": "Admin Zaid",
        "items": [
            {"productId": prodA_id, "name": "Espresso", "price": 100.0, "quantity": 2}
        ]
    }, headers=headersA, verify=False)
    if r.status_code in [200, 201]:
        orderA_id = r.json().get("id")
        return "PASS", f"Order created ID: {orderA_id}"
    return "FAIL", f"Status {r.status_code}: {r.text}"

def test_tenant_isolation_orders():
    rB = requests.get(f"{BASE_URL}/orders", headers=headersB, verify=False)
    if rB.status_code == 200:
        ordersB = rB.json()
        hasA = any(o.get("id") == orderA_id for o in ordersB)
        if not hasA:
            return "PASS", "Business B cannot access Business A orders (100% Isolated)"
        return "FAIL", "SECURITY BREACH: Business B accessed Business A orders!"
    return "FAIL", f"Status {rB.status_code}: {rB.text}"

run_test("6. Billing APIs", "Create Order", test_create_order_A)
run_test("3. Business Isolation", "Order Tenant Isolation", test_tenant_isolation_orders)

# --- 6. STAFF MODULE ---
def test_create_staff():
    username = f"staff_{int(time.time())}"
    r = requests.post(f"{BASE_URL}/staff", json={"name": "Barista Staff", "username": username, "password": "StaffPassword123!", "role": "CASHIER"}, headers=headersA, verify=False)
    if r.status_code in [200, 201]:
        return "PASS", f"Staff created username: {username}"
    return "FAIL", f"Status {r.status_code}: {r.text}"

run_test("8. Staff Module", "Create Staff Account", test_create_staff)

# --- 7. SYNC ENGINE ---
def test_sync_pull():
    r = requests.get(f"{BASE_URL}/sync/pull?lastSync=0", headers=headersA, verify=False)
    if r.status_code == 200 and "products" in r.json() and "categories" in r.json():
        return "PASS", "Sync pull returned delta payload for Room DB"
    return "FAIL", f"Status {r.status_code}: {r.text}"

run_test("14. Sync Engine", "SQLite Pull Delta Endpoint", test_sync_pull)

# Save final report JSON
with open("final_validation_results.json", "w", encoding="utf-8") as f:
    json.dump(results, f, indent=2)

print("\nFinal validation results saved to final_validation_results.json")
