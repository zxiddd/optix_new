import paramiko

hostname = "200.141.7.8"
username = "root"
password = "Zaiduddin@787"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    client.connect(hostname, username=username, password=password, timeout=10)
    
    # 1. Ensure /opt/optix/uploads directory exists on host
    client.exec_command("mkdir -p /opt/optix/uploads && chmod -R 777 /opt/optix/uploads")
    
    # 2. Read docker-compose.staging.yml
    stdin, stdout, stderr = client.exec_command("cat /opt/optix/infra/docker/docker-compose.staging.yml")
    content = stdout.read().decode('utf-8', errors='replace')
    
    # 3. Add volume mount if not present
    if "/opt/optix/uploads:/app/uploads" not in content:
        target = "    ports:\n      - \"127.0.0.1:3000:3000\""
        replacement = "    ports:\n      - \"127.0.0.1:3000:3000\"\n    volumes:\n      - /opt/optix/uploads:/app/uploads"
        new_content = content.replace(target, replacement)
        
        sftp = client.open_sftp()
        with sftp.file("/opt/optix/infra/docker/docker-compose.staging.yml", "w") as f:
            f.write(new_content)
        sftp.close()
        print("Updated docker-compose.staging.yml with persistent /opt/optix/uploads volume mount!")
    else:
        print("docker-compose.staging.yml already has /opt/optix/uploads volume mount!")
        
finally:
    client.close()
