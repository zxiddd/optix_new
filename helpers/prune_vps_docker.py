import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('200.141.7.8', username='root', password='Zaiduddin@787')

stdin, stdout, stderr = ssh.exec_command('docker builder prune -f')
out = stdout.read().decode('utf-8')
err = stderr.read().decode('utf-8')

print("PRUNE STDOUT:\n", out)
print("PRUNE STDERR:\n", err)

ssh.close()
