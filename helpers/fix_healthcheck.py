import paramiko

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect("200.141.7.8", username="root", password="Zaddy123")

cmd = "sed -i 's|http://localhost:3000/health|http://localhost:3000/api/v1/health|g' /opt/optix/infra/docker/docker-compose.staging.yml"
stdin, stdout, stderr = client.exec_command(cmd)
print("Out:", stdout.read().decode())
print("Err:", stderr.read().decode())

cmd_up = "cd /opt/optix/infra/docker && docker compose -f docker-compose.staging.yml up -d backend-staging"
stdin, stdout, stderr = client.exec_command(cmd_up)
print("Compose Up Out:", stdout.read().decode())
print("Compose Up Err:", stderr.read().decode())

client.close()
