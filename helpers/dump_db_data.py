import paramiko

vps_ip = "200.141.7.8"
vps_user = "root"
vps_pass = "Zaiduddin@787"

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(vps_ip, username=vps_user, password=vps_pass)

def query(sql):
    # Pass SQL directly via stdin to psql to avoid quote escaping issues
    cmd = 'docker exec -i optix-postgres-staging psql -U optix_staging_admin -d optix_staging_db'
    stdin, stdout, stderr = ssh.exec_command(cmd)
    stdin.write(sql + "\n")
    stdin.flush()
    stdin.channel.shutdown_write()
    out = stdout.read().decode()
    err = stderr.read().decode()
    return out

print("=== 1. BUSINESS TABLE ===")
print(query('SELECT id, name, email, phone, "createdAt" FROM "Business";'))

print("=== 2. USER TABLE ===")
print(query('SELECT id, email, role, "businessId", "createdAt" FROM "User";'))

print("=== 3. CATEGORY TABLE ===")
print(query('SELECT id, name, "businessId" FROM "Category";'))

print("=== 4. PRODUCT TABLE ===")
print(query('SELECT id, name, price, "businessId" FROM "Product";'))

print("=== 5. ORDER TABLE ===")
print(query('SELECT id, "invoiceNumber", total, "businessId" FROM "Order";'))

print("=== 6. RECEIPT SETTINGS TABLE ===")
print(query('SELECT id, "businessId", "showLogo", "logoUrl" FROM "ReceiptSettings";'))

ssh.close()
