import paramiko

hostname = "200.141.7.8"
username = "root"
password = "Zaddy123"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    client.connect(hostname, username=username, password=password, timeout=10)
    stdin, stdout, stderr = client.exec_command("ls -la /opt/optix; ls -la /opt/optix/infra/docker")
    print("Files in /opt/optix:\n", stdout.read().decode())
finally:
    client.close()
