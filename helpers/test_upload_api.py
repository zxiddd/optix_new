import paramiko
import json
import time

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect("200.141.7.8", username="root", password="Zaddy123")

email = f"uploader_{int(time.time())}@optixapp.in"
signup_cmd = f'curl -s -X POST http://localhost:3000/api/v1/auth/local/signup -H "Content-Type: application/json" -d "{{\\"email\\":\\"{email}\\", \\"password\\":\\"testpass123\\", \\"businessName\\":\\"Test Upload Store\\"}}"'
stdin, stdout, stderr = client.exec_command(signup_cmd)
resp = stdout.read().decode()

data = json.loads(resp)
token = data.get("access_token", "")
print("Access Token Generated:", token[:30], "...")

# 1. Test POST /api/v1/upload
upload_cmd = f'curl -s -X POST http://localhost:3000/api/v1/upload -H "Authorization: Bearer {token}" -F "file=@/opt/optix/package.json;type=image/png" -F "category=businesses"'
stdin, stdout, stderr = client.exec_command(upload_cmd)
up_resp = stdout.read().decode()
print("Upload Response JSON:\n", up_resp)

up_data = json.loads(up_resp)
returned_url = up_data.get("url", "")
print("Returned URL:", returned_url)

# 2. Test GET returned_url (static file serving)
fetch_cmd = f'curl -i {returned_url}'
stdin, stdout, stderr = client.exec_command(fetch_cmd)
fetch_resp = stdout.read().decode()
print("GET Image Response Header:\n", fetch_resp[:300])

client.close()
