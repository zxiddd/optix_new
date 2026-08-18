import paramiko

vps_ip = "200.141.7.8"
vps_user = "root"
vps_pass = "Zaddy123"

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(vps_ip, username=vps_user, password=vps_pass)

def run_db_query(query):
    cmd = f'docker exec -i optix-postgres-staging psql -U optix_staging_admin -d optix_staging_db -c "{query}"'
    stdin, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode()
    err = stderr.read().decode()
    return out, err

print("=== 1. BUSINESS TABLE ===")
out, err = run_db_query('SELECT id, name, email, phone, "createdAt" FROM "Business";')
print(out)

print("=== 2. USER TABLE ===")
out, err = run_db_query('SELECT id, email, role, "businessId", "createdAt" FROM "User";')
print(out)

print("=== 3. CATEGORY TABLE ===")
out, err = run_db_query('SELECT id, name, "businessId" FROM "Category";')
print(out)

print("=== 4. PRODUCT TABLE ===")
out, err = run_db_query('SELECT id, name, price, "businessId" FROM "Product";')
print(out)

print("=== 5. ORDER TABLE ===")
out, err = run_db_query('SELECT id, "invoiceNumber", total, "businessId" FROM "Order";')
print(out)

print("=== 6. RECEIPT SETTINGS TABLE ===")
out, err = run_db_query('SELECT id, "businessId", "showLogo", "logoUrl" FROM "ReceiptSettings";')
print(out)

ssh.close()
