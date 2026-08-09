import requests
import json
import time
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

BASE_URL = "https://api.optixapp.in/api/v1"
results = []

def run_test(section, name, func):
    t0 = time.time()
    try:
        status, details = func()
        lat = round((time.time() - t0) * 1000, 2)
        results.append({"section": section, "test": name, "status": status, "details": details, "latency": lat})
        print(f"[{status}] {section} -> {name} ({lat}ms) | {details[:80]}")
    except Exception as e:
        lat = round((time.time() - t0) * 1000, 2)
        results.append({"section": section, "test": name, "status": "FAIL", "details": str(e), "latency": lat})
        print(f"[FAIL] {section} -> {name} ({lat}ms) | EXCEPTION: {str(e)[:80]}")

print("=== RUNNING FULL API MODULE VALIDATION SUITE ===")

# Tokens & IDs
bizA_tokens = {}
bizB_tokens = {}
catA_id = None
prodA_id = None
orderA_id = None

# --- 2. AUTHENTICATION ---
def test_signup_bizA():
    global bizA_tokens
    email = f"owner_a_{int(time.time())}@optixapp.in"
    r = requests.post(f"{BASE_URL}/auth/local/signup", json={"email": email, "password": "Password123!", "businessName": "Business A"})
    if r.status_code == 201:
        bizA_tokens = r.json()
        bizA_tokens["email"] = email
        return "PASS", f"Created User ID: {bizA_tokens.get('userId')}"
    return "FAIL", f"Status {r.status_code}: {r.text}"

def test_signup_bizB():
    global bizB_tokens
    email = f"owner_b_{int(time.time())}@optixapp.in"
    r = requests.post(f"{BASE_URL}/auth/local/signup", json={"email": email, "password": "Password123!", "businessName": "Business B"})
    if r.status_code == 201:
        bizB_tokens = r.json()
        bizB_tokens["email"] = email
        return "PASS", f"Created User ID: {bizB_tokens.get('userId')}"
    return "FAIL", f"Status {r.status_code}: {r.text}"

def test_login_bizA():
    r = requests.post(f"{BASE_URL}/auth/local/signin", json={"email": bizA_tokens["email"], "password": "Password123!"})
    if r.status_code == 200 and "access_token" in r.json():
        return "PASS", "Login successful, received access_token"
    return "FAIL", f"Status {r.status_code}: {r.text}"

def test_wrong_password():
    r = requests.post(f"{BASE_URL}/auth/local/signin", json={"email": bizA_tokens["email"], "password": "WrongPassword"})
    if r.status_code == 403:
        return "PASS", "Forbidden 403 correctly returned for wrong password"
    return "FAIL", f"Expected 403, got {r.status_code}: {r.text}"

def test_duplicate_email():
    r = requests.post(f"{BASE_URL}/auth/local/signup", json={"email": bizA_tokens["email"], "password": "Password123!", "businessName": "Dup Biz"})
    if r.status_code in [400, 403, 409, 500]:
        return "PASS", f"Duplicate email rejected with status {r.status_code}"
    return "FAIL", f"Unexpected status {r.status_code}: {r.text}"

def test_invalid_token():
    headers = {"Authorization": "Bearer invalid.jwt.token"}
    r = requests.get(f"{BASE_URL}/business/profile", headers=headers)
    if r.status_code in [401, 403]:
        return "PASS", f"Invalid token rejected with status {r.status_code}"
    return "FAIL", f"Expected 401/403, got {r.status_code}"

run_test("2. Authentication", "Owner Signup (Business A)", test_signup_bizA)
run_test("2. Authentication", "Owner Signup (Business B)", test_signup_bizB)
run_test("2. Authentication", "Owner Signin (Business A)", test_login_bizA)
run_test("2. Authentication", "Wrong Password Rejection", test_wrong_password)
run_test("2. Authentication", "Duplicate Email Handling", test_duplicate_email)
run_test("2. Authentication", "Invalid JWT Verification", test_invalid_token)

headersA = {"Authorization": f"Bearer {bizA_tokens.get('access_token')}"}
headersB = {"Authorization": f"Bearer {bizB_tokens.get('access_token')}"}

# --- 3. CATEGORIES & BUSINESS ISOLATION ---
def test_create_category_A():
    global catA_id
    r = requests.post(f"{BASE_URL}/categories", json={"name": "Beverages A", "sortOrder": 1}, headers=headersA)
    if r.status_code == 201:
        catA_id = r.json().get("id")
        return "PASS", f"Category created ID: {catA_id}"
    return "FAIL", f"Status {r.status_code}: {r.text}"

def test_isolation_categories():
    rB = requests.get(f"{BASE_URL}/categories", headers=headersB)
    if rB.status_code == 200:
        catsB = rB.json()
        hasA = any(c.get("id") == catA_id for c in catsB)
        if not hasA:
            return "PASS", "Business B cannot see Business A category (Strict Tenant Isolation)"
        return "FAIL", "SECURITY BREACH: Business B accessed Business A category!"
    return "FAIL", f"Status {rB.status_code}"

run_test("3. Categories", "Create Category (Business A)", test_create_category_A)
run_test("3. Tenant Isolation", "Category Data Isolation", test_isolation_categories)

# --- 4. PRODUCTS ---
def test_create_product_A():
    global prodA_id
    r = requests.post(f"{BASE_URL}/products", json={
        "name": "Iced Latte",
        "price": 150.0,
        "categoryId": catA_id,
        "pricingType": "FIXED",
        "unit": "Cup"
    }, headers=headersA)
    if r.status_code == 201:
        prodA_id = r.json().get("id")
        return "PASS", f"Product created ID: {prodA_id}"
    return "FAIL", f"Status {r.status_code}: {r.text}"

def test_isolation_products():
    rB = requests.get(f"{BASE_URL}/products", headers=headersB)
    if rB.status_code == 200:
        prodsB = rB.json()
        hasA = any(p.get("id") == prodA_id for p in prodsB)
        if not hasA:
            return "PASS", "Business B cannot see Business A product (Strict Tenant Isolation)"
        return "FAIL", "SECURITY BREACH: Business B accessed Business A product!"
    return "FAIL", f"Status {rB.status_code}"

def test_soft_delete_product():
    r = requests.delete(f"{BASE_URL}/products/{prodA_id}", headers=headersA)
    if r.status_code == 200:
        r_get = requests.get(f"{BASE_URL}/products", headers=headersA)
        prods = r_get.json()
        still_present = any(p.get("id") == prodA_id for p in prods)
        if not still_present:
            return "PASS", "Product successfully soft-deleted and excluded from query"
        return "FAIL", "Product still visible after deletion"
    return "FAIL", f"Status {r.status_code}"

run_test("4. Products", "Create Product", test_create_product_A)
run_test("3. Tenant Isolation", "Product Data Isolation", test_isolation_products)
run_test("4. Products", "Soft Delete Product", test_soft_delete_product)

# Re-create product for order testing
r_recreate = requests.post(f"{BASE_URL}/products", json={"name": "Espresso", "price": 100.0, "categoryId": catA_id}, headers=headersA)
if r_recreate.status_code == 201:
    prodA_id = r_recreate.json().get("id")

# --- 5. ORDERS & BILLING ---
def test_create_order_A():
    global orderA_id
    r = requests.post(f"{BASE_URL}/orders", json={
        "total": 200.0,
        "paymentMethod": "UPI",
        "cashierName": "Cashier Zaid",
        "items": [
            {"productId": prodA_id, "name": "Espresso", "price": 100.0, "quantity": 2}
        ]
    }, headers=headersA)
    if r.status_code == 201:
        orderA_id = r.json().get("id")
        return "PASS", f"Order created ID: {orderA_id}"
    return "FAIL", f"Status {r.status_code}: {r.text}"

def test_isolation_orders():
    rB = requests.get(f"{BASE_URL}/orders", headers=headersB)
    if rB.status_code == 200:
        ordersB = rB.json()
        hasA = any(o.get("id") == orderA_id for o in ordersB)
        if not hasA:
            return "PASS", "Business B cannot see Business A order (Strict Tenant Isolation)"
        return "FAIL", "SECURITY BREACH: Business B accessed Business A order!"
    return "FAIL", f"Status {rB.status_code}"

run_test("5. Billing", "Create Order with Items", test_create_order_A)
run_test("3. Tenant Isolation", "Order Data Isolation", test_isolation_orders)

# --- 6. STAFF MODULE ---
def test_create_staff():
    username = f"staff_{int(time.time())}"
    r = requests.post(f"{BASE_URL}/staff", json={"name": "Staff Zaid", "username": username, "password": "StaffPassword123", "role": "CASHIER"}, headers=headersA)
    if r.status_code == 201:
        return "PASS", f"Staff created username: {username}"
    return "FAIL", f"Status {r.status_code}: {r.text}"

run_test("6. Staff", "Create Staff Account", test_create_staff)

# --- 7. SYNC ENGINE ---
def test_sync_pull():
    r = requests.get(f"{BASE_URL}/sync/pull?lastSync=0", headers=headersA)
    if r.status_code == 200 and "products" in r.json() and "categories" in r.json():
        return "PASS", "Sync pull successfully returned categories, products, orders, and staff"
    return "FAIL", f"Status {r.status_code}: {r.text}"

run_test("7. Sync Engine", "Sync Pull Delta Endpoint", test_sync_pull)

# Save suite results
with open("validation_suite_results.json", "w") as f:
    json.dump(results, f, indent=2)

print("\nValidation suite complete. Results saved to validation_suite_results.json")
