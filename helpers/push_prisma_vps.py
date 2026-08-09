import paramiko, sys

vps_ip = "200.141.7.8"
vps_user = "root"
vps_pass = "Zaiduddin@787"

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(vps_ip, username=vps_user, password=vps_pass)

stdin, stdout, stderr = ssh.exec_command('docker exec optix-backend-staging npx prisma db push --accept-data-loss')
out = stdout.read().decode('utf-8', errors='ignore')
err = stderr.read().decode('utf-8', errors='ignore')
sys.stdout.reconfigure(encoding='utf-8')
print("STDOUT:\n", out)
print("STDERR:\n", err)

ssh.close()
