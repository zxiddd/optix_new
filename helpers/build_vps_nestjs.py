import paramiko
import os

hostname = "200.141.7.8"
username = "root"
password = "Zaiduddin@787"

local_backend = r"c:\Users\zaid\StudioProjects\pos\backend"
remote_backend = "/opt/optix/apps/backend"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    print("Connecting to VPS...")
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

    print("Uploading fixed codebase...")
    upload_dir(local_backend, remote_backend)
    sftp.close()

    print("Building NestJS Docker image (--no-cache)...")
    cmd = "cd /opt/optix/apps/backend && docker build --no-cache -t optix-nestjs-backend:latest ."
    stdin, stdout, stderr = client.exec_command(cmd)
    
    out_str = stdout.read().decode('utf-8', errors='ignore')
    err_str = stderr.read().decode('utf-8', errors='ignore')
    print("Build output length:", len(out_str))
    if "ERROR" in err_str or "error" in err_str.lower():
        print("Build stderr summary:\n", err_str[-500:])

    print("Restarting optix-backend-staging container...")
    run_cmd = """
    docker stop optix-backend-staging || true
    docker rm optix-backend-staging || true
    docker run -d --name optix-backend-staging \
      --network docker_optix-staging-network \
      -p 127.0.0.1:3000:3000 \
      -e NODE_ENV=production \
      -e PORT=3000 \
      -e DATABASE_URL=postgresql://optix_staging_admin:optix_staging_pass_9022@optix-postgres-staging:5432/optix_staging_db?schema=public \
      -e REDIS_URL=redis://optix-redis-staging:6379 \
      -e JWT_SECRET=optix_staging_jwt_secret_9022_key_32bytes \
      -e JWT_ACCESS_SECRET=optix_staging_jwt_access_secret_9022_key_32bytes \
      -e JWT_REFRESH_SECRET=optix_staging_jwt_refresh_secret_9022_key_32bytes \
      --restart always \
      optix-nestjs-backend:latest
    """
    stdin, stdout, stderr = client.exec_command(run_cmd)
    print("Run stdout:", stdout.read().decode('utf-8', errors='ignore'))
    print("Run stderr:", stderr.read().decode('utf-8', errors='ignore'))

finally:
    client.close()
