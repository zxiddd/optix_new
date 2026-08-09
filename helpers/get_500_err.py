import paramiko

vps_ip = "200.141.7.8"
vps_user = "root"
vps_pass = "Zaiduddin@787"

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(vps_ip, username=vps_user, password=vps_pass)

stdin, stdout, stderr = ssh.exec_command('docker logs --tail 50 optix-backend-staging')
print("DOCKER LOGS:\n", stdout.read().decode('utf-8', errors='ignore'))
print("STDERR:\n", stderr.read().decode('utf-8', errors='ignore'))

ssh.close()
