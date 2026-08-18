import paramiko

vps_ip = "200.141.7.8"
vps_user = "root"
vps_pass = "Zaddy123"

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(vps_ip, username=vps_user, password=vps_pass)

stdin, stdout, stderr = ssh.exec_command('docker exec optix-backend-staging env | grep DATABASE_URL')
print("DATABASE_URL:", stdout.read().decode())

stdin, stdout, stderr = ssh.exec_command('docker exec optix-postgres-staging psql -U postgres -l')
print("DATABASES:\n", stdout.read().decode())

ssh.close()
