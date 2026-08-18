import paramiko

hostname = "200.141.7.8"
username = "root"
password = "Zaddy123"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    client.connect(hostname, username=username, password=password, timeout=10)
    stdin, stdout, stderr = client.exec_command("docker exec optix-backend-staging npx prisma db push --accept-data-loss")
    out_str = stdout.read().decode('utf-8', errors='replace').encode('ascii', 'ignore').decode('ascii')
    err_str = stderr.read().decode('utf-8', errors='replace').encode('ascii', 'ignore').decode('ascii')
    print("Prisma DB Push Output:\n", out_str)
    print("Prisma DB Push Error:\n", err_str)
finally:
    client.close()
