import requests
import json

base_url = "https://api.optixapp.in"

print("--- 1. Testing Health Endpoints ---")
r = requests.get(f"{base_url}/health", verify=False)
print("Health status:", r.status_code, r.text)

r_db = requests.get(f"{base_url}/health/db", verify=False)
print("DB Health status:", r_db.status_code, r_db.text)

r_docs = requests.get(f"{base_url}/docs", verify=False)
print("Swagger docs status:", r_docs.status_code)

print("\n--- 2. Testing Auth Signup & Signin ---")
test_email = f"test_owner_{int(requests.get(f'{base_url}/health').json()['data']['timestamp'])}@optixapp.in"
signup_payload = {
    "email": test_email,
    "password": "Password@123",
    "businessName": "Test Optix Restaurant"
}
r_signup = requests.post(f"{base_url}/api/v1/auth/local/signup", json=signup_payload, verify=False)
print("Signup response:", r_signup.status_code, r_signup.text)

tokens = r_signup.json()
access_token = tokens.get("access_token")
headers = {"Authorization": f"Bearer {access_token}"}

print("\n--- 3. Testing Business Profile ---")
r_prof = requests.get(f"{base_url}/api/v1/business/profile", headers=headers, verify=False)
print("Profile status:", r_prof.status_code, r_prof.text[:200])

print("\n--- 4. Testing Categories ---")
cat_payload = {"name": "Beverages", "sortOrder": 1}
r_cat = requests.post(f"{base_url}/api/v1/categories", json=cat_payload, headers=headers, verify=False)
print("Create category status:", r_cat.status_code, r_cat.text)
cat_id = r_cat.json().get("id")

r_cats = requests.get(f"{base_url}/api/v1/categories", headers=headers, verify=False)
print("Get categories status:", r_cats.status_code, r_cats.text[:200])

print("\n--- 5. Testing Products ---")
prod_payload = {
    "name": "Cold Coffee",
    "price": 120.00,
    "categoryId": cat_id,
    "pricingType": "FIXED",
    "unit": "Cup"
}
r_prod = requests.post(f"{base_url}/api/v1/products", json=prod_payload, headers=headers, verify=False)
print("Create product status:", r_prod.status_code, r_prod.text)
prod_id = r_prod.json().get("id")

r_prods = requests.get(f"{base_url}/api/v1/products", headers=headers, verify=False)
print("Get products status:", r_prods.status_code, r_prods.text[:200])

print("\n--- 6. Testing Orders ---")
order_payload = {
    "subtotal": 120.00,
    "total": 120.00,
    "paymentMethod": "UPI",
    "cashierName": "Manager",
    "items": [
        {"productId": prod_id, "name": "Cold Coffee", "price": 120.00, "quantity": 1}
    ]
}
r_order = requests.post(f"{base_url}/api/v1/orders", json=order_payload, headers=headers, verify=False)
print("Create order status:", r_order.status_code, r_order.text)

r_orders = requests.get(f"{base_url}/api/v1/orders", headers=headers, verify=False)
print("Get orders status:", r_orders.status_code, r_orders.text[:200])

print("\n--- 7. Testing Sync Pull ---")
r_sync = requests.get(f"{base_url}/api/v1/sync/pull?lastSync=0", headers=headers, verify=False)
print("Sync pull status:", r_sync.status_code, r_sync.text[:200])
