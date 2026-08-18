import paramiko

hostname = "200.141.7.8"
username = "root"
password = "Zaddy123"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    print(f"Connecting to {hostname}...")
    client.connect(hostname, username=username, password=password, timeout=10)
    print("Connected successfully!")
    stdin, stdout, stderr = client.exec_command("uname -a; uptime; docker -v || true; node -v || true")
    print("Output:\n", stdout.read().decode())
    print("Error:\n", stderr.read().decode())
finally:
    client.close()
