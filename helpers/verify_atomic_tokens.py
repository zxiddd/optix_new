import requests, json, sys

sys.stdout.reconfigure(encoding='utf-8')

url = "https://api.optixapp.in/api/v1"

# 1. Login
resp = requests.post(f"{url}/auth/google", json={"email": "zaiduddin787@gmail.com", "name": "Zaiduddin 787", "googleId": "test_787"}, verify=False)
auth_data = resp.json()
print("AUTH RESP:", auth_data)

token = auth_data["access_token"]
headers = {"Authorization": f"Bearer {token}"}

# 2. Create Order 1 (Device A)
o1 = requests.post(f"{url}/orders", json={
    "invoiceNumber": "INV-20260804-101",
    "total": 50.0,
    "paymentMethod": "CASH",
    "items": [{"productId": "prod_test_787", "productName": "Special Masala Chai", "price": 50.0, "quantity": 1}]
}, headers=headers, verify=False).json()

# 3. Create Order 2 (Device B)
o2 = requests.post(f"{url}/orders", json={
    "invoiceNumber": "INV-20260804-102",
    "total": 100.0,
    "paymentMethod": "UPI",
    "items": [{"productId": "prod_test_787", "productName": "Special Masala Chai", "price": 50.0, "quantity": 2}]
}, headers=headers, verify=False).json()

# 4. Create Order 3 (Device A again)
o3 = requests.post(f"{url}/orders", json={
    "invoiceNumber": "INV-20260804-103",
    "total": 150.0,
    "paymentMethod": "CARD",
    "items": [{"productId": "prod_test_787", "productName": "Special Masala Chai", "price": 50.0, "quantity": 3}]
}, headers=headers, verify=False).json()

print("====================================================")
print("ATOMIC TRANSACTION-SAFE TOKEN GENERATION RESULTS")
print("====================================================")
print("Order 1 (Device A) Token:", o1.get("tokenNumber"))
print("Order 2 (Device B) Token:", o2.get("tokenNumber"))
print("Order 3 (Device A) Token:", o3.get("tokenNumber"))
