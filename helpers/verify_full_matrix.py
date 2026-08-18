import paramiko, requests, json, sys

sys.stdout.reconfigure(encoding='utf-8')

url = "https://api.optixapp.in/api/v1"

# 1. Login & Get Access Token
resp = requests.post(f"{url}/auth/google", json={"email": "zaiduddin787@gmail.com", "name": "Zaiduddin 787", "googleId": "test_787"}, verify=False)
auth_data = resp.json()
token = auth_data["access_token"]
headers = {"Authorization": f"Bearer {token}"}

# 2. Save Business Profile & Receipt Settings
profile_payload = {
    "name": "Zaddy's Optix Store",
    "phone": "9876543210",
    "address": "Hyderabad, Telangana",
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
    }
}
requests.post(f"{url}/business/profile", json=profile_payload, headers=headers, verify=False)

# 3. Save a Category
cat_resp = requests.post(f"{url}/categories", json={"id": "cat_test_787", "name": "Hot Beverages", "sortOrder": 1}, headers=headers, verify=False)

# 4. Save a Product
prod_resp = requests.post(f"{url}/products", json={"id": "prod_test_787", "name": "Special Masala Chai", "price": 40.0, "categoryId": "cat_test_787"}, headers=headers, verify=False)

# 5. Save an Order with OrderItems
order_payload = {
    "id": "ord_test_787",
    "tokenNumber": "007",
    "invoiceNumber": "INV-20260804-007",
    "subtotal": 40.0,
    "discount": 0.0,
    "tax": 2.0,
    "total": 42.0,
    "paymentMethod": "Upi",
    "cashierName": "Admin",
    "orderItemsJson": json.dumps([{"itemId": "prod_test_787", "itemName": "Special Masala Chai", "price": 40.0, "quantity": 1}]),
    "items": [{"productId": "prod_test_787", "productName": "Special Masala Chai", "price": 40.0, "quantity": 1}]
}
requests.post(f"{url}/orders", json=order_payload, headers=headers, verify=False)

# 6. Fetch Live /sync/full-dump
dump_resp = requests.get(f"{url}/sync/full-dump", headers=headers, verify=False)
dump_data = dump_resp.json()

print("====================================================")
print("LIVE /sync/full-dump VERIFICATION RESPONSE")
print("====================================================")
print(json.dumps(dump_data, indent=2))

# 7. Query PostgreSQL Direct DB Records
vps_ip = "200.141.7.8"
vps_user = "root"
vps_pass = "Zaddy123"

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(vps_ip, username=vps_user, password=vps_pass)

def query(sql):
    cmd = 'docker exec -i optix-postgres-staging psql -U optix_staging_admin -d optix_staging_db'
    stdin, stdout, stderr = ssh.exec_command(cmd)
    stdin.write(sql + "\n")
    stdin.flush()
    stdin.channel.shutdown_write()
    out = stdout.read().decode('utf-8', errors='ignore')
    return out

print("\n====================================================")
print("POSTGRESQL RECORD DUMP")
print("====================================================")

print("\n--- BUSINESS TABLE ---")
print(query('SELECT id, name, email, phone, address, "setupCompleted" FROM "Business" WHERE email=\'zaiduddin787@gmail.com\';'))

print("\n--- RECEIPT SETTINGS TABLE ---")
print(query('SELECT * FROM "ReceiptSettings";'))

print("\n--- CATEGORY TABLE ---")
print(query('SELECT * FROM "Category";'))

print("\n--- PRODUCT TABLE ---")
print(query('SELECT id, name, price, "categoryId", "businessId" FROM "Product";'))

print("\n--- ORDER TABLE ---")
print(query('SELECT id, "invoiceNumber", "tokenNumber", total, "paymentMethod", "businessId" FROM "Order";'))

print("\n--- ORDER ITEMS TABLE ---")
print(query('SELECT id, "orderId", "productName", price, quantity FROM "OrderItem";'))

ssh.close()
