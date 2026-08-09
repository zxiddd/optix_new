import requests, json, sys, time

sys.stdout.reconfigure(encoding='utf-8')

url = "https://api.optixapp.in/api/v1"

# 1. Login Device A
respA = requests.post(f"{url}/auth/google", json={"email": "zaiduddin787@gmail.com", "name": "Zaiduddin 787", "googleId": "test_787"}, verify=False).json()
tokenA = respA["access_token"]
headersA = {"Authorization": f"Bearer {tokenA}"}

# 2. Login Device B (Same Business Account)
respB = requests.post(f"{url}/auth/google", json={"email": "zaiduddin787@gmail.com", "name": "Zaiduddin 787", "googleId": "test_787"}, verify=False).json()
tokenB = respB["access_token"]
headersB = {"Authorization": f"Bearer {tokenB}"}

t_start = int(time.time() * 1000) - 5000

# 3. Device A creates 3 new orders
for i in range(1, 4):
    requests.post(f"{url}/orders", json={
        "invoiceNumber": f"INV-MULTI-{i}",
        "total": 100.0 * i,
        "paymentMethod": "UPI",
        "items": [{"productId": "prod_test_787", "productName": f"Product {i}", "price": 100.0 * i, "quantity": 1}]
    }, headers=headersA, verify=False)

# 4. Device B calls /sync/pull?since=t_start
pull_res = requests.get(f"{url}/sync/pull?since={t_start}", headers=headersB, verify=False).json()
synced_orders = pull_res.get("orders", [])

print("====================================================")
print("MULTI-DEVICE INCREMENTAL DELTA SYNC TEST RESULTS")
print("====================================================")
print("Orders Pulled by Device B:", len(synced_orders))
for o in synced_orders:
    print(f" -> Order Invoice: {o.get('invoiceNumber')}, Official Token: {o.get('tokenNumber')}")

# 5. Offline Temp Token Resolution Test
temp_order = requests.post(f"{url}/orders", json={
    "tokenNumber": "TEMP-101",
    "invoiceNumber": "INV-OFFLINE-001",
    "total": 250.0,
    "paymentMethod": "CASH",
    "items": [{"productId": "prod_test_787", "productName": "Offline Chai", "price": 250.0, "quantity": 1}]
}, headers=headersA, verify=False).json()

print("\n====================================================")
print("OFFLINE TEMP TOKEN RESOLUTION TEST RESULTS")
print("====================================================")
print("Submitted Temp Token: TEMP-101")
print("Resolved Official Token from PostgreSQL:", temp_order.get("tokenNumber"))
