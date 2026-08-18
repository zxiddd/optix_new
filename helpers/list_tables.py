import paramiko

vps_ip = "200.141.7.8"
vps_user = "root"
vps_pass = "Zaddy123"

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(vps_ip, username=vps_user, password=vps_pass)

stdin, stdout, stderr = ssh.exec_command('docker exec optix-postgres-staging psql -U optix_staging_admin -d optix_staging_db -c "\\dt"')
print("STDOUT:\n", stdout.read().decode())
print("STDERR:\n", stderr.read().decode())

ssh.close()
