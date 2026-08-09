import paramiko

hostname = "200.141.7.8"
username = "root"
password = "Zaiduddin@787"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    client.connect(hostname, username=username, password=password, timeout=10)
    stdin, stdout, stderr = client.exec_command("rm -rf /opt/optix/apps/backend/*; cp -r /opt/optix/apps/backend/.. /opt/optix/apps/backend_backup 2>/dev/null || true")
    print("Cleaned VPS backend dir:\n", stdout.read().decode())
finally:
    client.close()
