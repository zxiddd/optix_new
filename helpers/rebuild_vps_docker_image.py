import paramiko

hostname = "200.141.7.8"
username = "root"
password = "Zaddy123"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    client.connect(hostname, username=username, password=password, timeout=10)
    stdin, stdout, stderr = client.exec_command("cd /opt/optix/infra/docker && docker compose -f docker-compose.staging.yml build --no-cache backend-staging && docker compose -f docker-compose.staging.yml up -d backend-staging")
    print("Docker rebuild output:\n", stdout.read().decode())
    print("Docker rebuild error:\n", stderr.read().decode())
finally:
    client.close()
