import paramiko, requests, json, sys

sys.stdout.reconfigure(encoding='utf-8')

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

print("====================================================")
print("1. POSTGRESQL TABLES VERIFICATION")
print("====================================================")

print("\n--- BUSINESS TABLE ---")
print(query('SELECT id, name, email, phone, address, "setupCompleted", "createdAt" FROM "Business";'))

print("\n--- USER TABLE ---")
print(query('SELECT id, email, role, "businessId", "createdAt" FROM "User";'))

print("\n--- CATEGORY TABLE ---")
print(query('SELECT id, name, "businessId" FROM "Category";'))

print("\n--- PRODUCT TABLE ---")
print(query('SELECT id, name, price, "businessId" FROM "Product";'))

print("\n--- ORDER TABLE ---")
print(query('SELECT id, "invoiceNumber", total, "businessId" FROM "Order";'))

print("\n--- RECEIPT SETTINGS TABLE ---")
print(query('SELECT id, "businessId", "showLogo", "logoUrl", "footerMessage" FROM "ReceiptSettings";'))

ssh.close()

# Test live API authentication & full dump for zaiduddin787@gmail.com
url = "https://api.optixapp.in/api/v1/auth/google"
payload = {"email": "zaiduddin787@gmail.com", "name": "Zaiduddin 787", "googleId": "test_google_787"}
resp = requests.post(url, json=payload, verify=False)
data = resp.json()

print("====================================================")
print("2. LIVE AUTHENTICATION RESPONSE FOR zaiduddin787@gmail.com")
print("====================================================")
print(json.dumps(data, indent=2))

if "access_token" in data:
    token = data["access_token"]
    dump_resp = requests.get("https://api.optixapp.in/api/v1/sync/full-dump", headers={"Authorization": f"Bearer {token}"}, verify=False)
    print("\n====================================================")
    print("3. LIVE /sync/full-dump RESPONSE")
    print("====================================================")
    print(json.dumps(dump_resp.json(), indent=2))
