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
            local_item = os.path.join(local_path, item)
            remote_item = f"{remote_path}/{item}"
            if os.path.isdir(local_item):
                upload_dir(local_item, remote_item)
            else:
                print(f"Uploading {local_item} -> {remote_item}")
                sftp.put(local_item, remote_item)

    print("Uploading backend files to VPS...")
    upload_dir(os.path.join(local_backend, "src"), f"{remote_backend}/src")
    upload_dir(os.path.join(local_backend, "prisma"), f"{remote_backend}/prisma")
    sftp.put(os.path.join(local_backend, "package.json"), f"{remote_backend}/package.json")
    
    # Ensure uploads directory on VPS
    try:
        sftp.mkdir("/opt/optix/uploads")
        sftp.mkdir("/opt/optix/uploads/businesses")
        sftp.mkdir("/opt/optix/uploads/products")
    except IOError:
        pass

    sftp.close()

    print("Executing database migration & build on VPS...")
    commands = [
        f"cd {remote_backend} && npx prisma generate && npx prisma db push --accept-data-loss || true",
        "docker restart optix-backend-staging || true",
        "curl -k -i https://api.optixapp.in/health"
    ]

    for cmd in commands:
        print(f"Running: {cmd}")
        stdin, stdout, stderr = client.exec_command(cmd)
        print("Output:", stdout.read().decode())
        print("Error:", stderr.read().decode())

finally:
    client.close()
