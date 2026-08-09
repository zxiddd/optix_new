import paramiko

hostname = "200.141.7.8"
username = "root"
password = "Zaiduddin@787"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    client.connect(hostname, username=username, password=password, timeout=10)
    stdin, stdout, stderr = client.exec_command("docker exec -w /app/apps/backend optix-backend-staging npm run build || docker exec optix-backend-staging npm run build || true; docker restart optix-backend-staging")
    print("Build output:\n", stdout.read().decode())
    print("Build error:\n", stderr.read().decode())
finally:
    client.close()
