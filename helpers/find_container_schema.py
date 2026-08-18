import paramiko

hostname = "200.141.7.8"
username = "root"
password = "Zaddy123"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    client.connect(hostname, username=username, password=password, timeout=10)
    stdin, stdout, stderr = client.exec_command("docker exec optix-backend-staging pwd; docker exec optix-backend-staging find /app -name 'schema.prisma'")
    print("Container path:\n", stdout.read().decode())
    print("Container error:\n", stderr.read().decode())
finally:
    client.close()
