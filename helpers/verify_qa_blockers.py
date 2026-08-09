import requests, json, sys, time

sys.stdout.reconfigure(encoding='utf-8')

url = "https://api.optixapp.in/api/v1"

# 1. Test Brand New User Business Setup Detection
new_email = f"brandnew_{int(time.time())}@gmail.com"
resp_new = requests.post(f"{url}/auth/google", json={"email": new_email, "name": "Brand New User", "googleId": f"g_{int(time.time())}"}, verify=False).json()

print("====================================================")
print("TEST 1: BRAND NEW GOOGLE USER BUSINESS SETUP DETECTION")
print("====================================================")
print("New Email:", new_email)
print("setupCompleted returned:", resp_new.get("setupCompleted"))

# 2. Login Existing User
resp_exist = requests.post(f"{url}/auth/google", json={"email": "zaiduddin787@gmail.com", "name": "Zaiduddin 787", "googleId": "test_787"}, verify=False).json()
token = resp_exist["access_token"]
headers = {"Authorization": f"Bearer {token}"}

# 3. Test Payment QR & Receipt Settings Persistence
qr_id = f"qr_upi_{int(time.time())}"
profile_payload = {
    "name": "Zaddy's Optix Flagship Store",
    "phone": "9876543210",
    "address": "Banjara Hills, Hyderabad",
    "receiptSettings": {
        "showLogo": True,
        "logoUrl": "https://api.optixapp.in/uploads/logo.png",
        "footerMessage": "Thank You! Visit Again 🙏",
        "showBusinessName": True,
        "showAddress": True,
        "showPhone": True,
        "showGst": True,
        "showDateTime": True,
        "showOrderNumber": True,
        "showCashierName": True,
        "showDiscounts": True,
        "showTaxes": True,
        "taxPercentage": 5.0,
        "qrEnabled": True,
        "showVisitAgain": True
    },
    "paymentQrs": [
        {
            "id": qr_id,
            "name": "Zaid's GPay UPI QR",
            "upiId": "zaid@okaxis",
            "imageUrl": "https://api.optixapp.in/uploads/qr_code.png",
            "isActive": True
        }
    ]
}
requests.post(f"{url}/business/profile", json=profile_payload, headers=headers, verify=False)

dump_res = requests.get(f"{url}/sync/full-dump", headers=headers, verify=False).json()
b_data = dump_res.get("business", {})
qrs_data = dump_res.get("paymentQrs", [])

print("\n====================================================")
print("TEST 2: RECEIPT SETTINGS & LOGO PERSISTENCE")
print("====================================================")
print("Business Name:", b_data.get("name"))
print("setupCompleted in PostgreSQL:", b_data.get("setupCompleted"))
print("Receipt Settings:", json.dumps(b_data.get("receiptSettings"), indent=2))

print("\n====================================================")
print("TEST 3: PAYMENT QR PERSISTENCE & SYNC")
print("====================================================")
print("Payment QRs array:", json.dumps(qrs_data, indent=2))

# 4. Test Offline Temp Token Resolution
inv_num = f"INV-OFFLINE-{int(time.time())}"
temp_order = requests.post(f"{url}/orders", json={
    "tokenNumber": "LOCAL-999",
    "invoiceNumber": inv_num,
    "total": 300.0,
    "paymentMethod": "UPI",
    "items": [{"productId": "prod_test_787", "productName": "Temp Chai", "price": 300.0, "quantity": 1}]
}, headers=headers, verify=False).json()

print("\n====================================================")
print("TEST 4: OFFLINE TEMP TOKEN RESOLUTION")
print("====================================================")
print("Submitted Temp Token: LOCAL-999")
print("Returned Official Atomic Token:", temp_order.get("tokenNumber"))
