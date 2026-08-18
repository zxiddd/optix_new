import paramiko

hostname = "200.141.7.8"
username = "root"
password = "Zaddy123"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    client.connect(hostname, username=username, password=password, timeout=10)
    stdin, stdout, stderr = client.exec_command("docker ps; docker logs optix-backend-staging --tail 30")
    print("Docker Output:\n", stdout.read().decode('utf-8', errors='replace'))
finally:
    client.close()
