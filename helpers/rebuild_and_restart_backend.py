import paramiko
import os

hostname = "200.141.7.8"
username = "root"
password = "Zaddy123"

local_backend = r"c:\Users\zaid\StudioProjects\pos\backend"
remote_backend = "/opt/optix/apps/backend"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    print(f"Connecting to {hostname}...")
    client.connect(hostname, username=username, password=password, timeout=10)
    sftp = client.open_sftp()

    def upload_dir(local_path, remote_path):
        try:
            sftp.mkdir(remote_path)
        except IOError:
            pass

        for item in os.listdir(local_path):
            if item in ["node_modules", ".git", "dist"]:
                continue
            local_item = os.path.join(local_path, item)
            remote_item = f"{remote_path}/{item}"
            if os.path.isdir(local_item):
                upload_dir(local_item, remote_item)
            else:
                sftp.put(local_item, remote_item)

    print("Uploading NestJS backend codebase...")
    upload_dir(local_backend, remote_backend)
    upload_dir(local_backend, "/opt/optix")

    sftp.put(os.path.join(local_backend, "Dockerfile"), "/opt/optix/Dockerfile.backend")
    sftp.close()

    print("Rebuilding NestJS backend container on VPS...")
    cmd = "cd /opt/optix/infra/docker && docker compose -f docker-compose.staging.yml build --no-cache backend-staging && docker compose -f docker-compose.staging.yml up -d --force-recreate backend-staging"
    stdin, stdout, stderr = client.exec_command(cmd)
    
    out_str = stdout.read().decode('utf-8', errors='replace')
    err_str = stderr.read().decode('utf-8', errors='replace')
    print("Build Output:\n", out_str.encode('ascii', 'ignore').decode('ascii'))
    print("Build Error:\n", err_str.encode('ascii', 'ignore').decode('ascii'))

    stdin, stdout, stderr = client.exec_command("docker ps; docker logs optix-backend-staging --tail 30")
    print("Docker Status & Logs:\n", stdout.read().decode('utf-8', errors='replace').encode('ascii', 'ignore').decode('ascii'))

finally:
    client.close()
