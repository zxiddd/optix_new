import paramiko
import sys

sys.stdout.reconfigure(encoding='utf-8')

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect("200.141.7.8", username="root", password="Zaiduddin@787")

cmd = 'docker exec optix-postgres-staging psql -U optix_staging_admin -d optix_staging_db -c "SELECT * FROM \\"ReceiptSettings\\" LIMIT 2; SELECT * FROM \\"PaymentQr\\" LIMIT 2; SELECT count(*) FROM \\"Order\\";"'
stdin, stdout, stderr = client.exec_command(cmd)
print("Out:\n", stdout.read().decode('utf-8', errors='ignore'))
print("Err:\n", stderr.read().decode('utf-8', errors='ignore'))

client.close()
