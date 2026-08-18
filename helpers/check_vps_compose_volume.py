import paramiko

hostname = "200.141.7.8"
username = "root"
password = "Zaddy123"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    client.connect(hostname, username=username, password=password, timeout=10)
    stdin, stdout, stderr = client.exec_command("cat /opt/optix/infra/docker/docker-compose.staging.yml")
    print("VPS Docker Compose Staging File:\n", stdout.read().decode('utf-8', errors='replace'))
finally:
    client.close()
