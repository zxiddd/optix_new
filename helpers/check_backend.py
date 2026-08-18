import paramiko

hostname = "200.141.7.8"
username = "root"
password = "Zaddy123"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    client.connect(hostname, username=username, password=password, timeout=10)
    stdin, stdout, stderr = client.exec_command("curl -k -i https://api.optixapp.in/health || curl -i http://127.0.0.1:3000/health || true; docker logs --tail 30 optix-backend-staging")
    print("Backend check:\n", stdout.read().decode())
    print("Error:\n", stderr.read().decode())
finally:
    client.close()
