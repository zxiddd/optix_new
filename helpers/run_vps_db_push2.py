import paramiko

hostname = "200.141.7.8"
username = "root"
password = "Zaiduddin@787"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    client.connect(hostname, username=username, password=password, timeout=10)
    stdin, stdout, stderr = client.exec_command("docker exec -w /app/apps/backend optix-backend-staging npx prisma db push --accept-data-loss; docker restart optix-backend-staging; sleep 2; curl -k -i https://api.optixapp.in/health")
    print("Database Migration Output:\n", stdout.read().decode())
    print("Error Output:\n", stderr.read().decode())
finally:
    client.close()
