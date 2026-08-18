import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('200.141.7.8', username='root', password='Zaddy123')

def run_cmd(cmd):
    stdin, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode('utf-8')
    err = stderr.read().decode('utf-8')
    return out, err

print("=== 1. DOCKER SYSTEM DF (BEFORE) ===")
out1, err1 = run_cmd("docker system df")
print(out1)
if err1: print("ERR:", err1)

print("\n=== 2. DOCKER BUILDER PRUNE -A -F ===")
out2, err2 = run_cmd("docker builder prune -a -f")
print(out2[:500] if len(out2) > 500 else out2)
if err2: print("ERR:", err2)

print("\n=== 3. DOCKER SYSTEM DF (AFTER) ===")
out3, err3 = run_cmd("docker system df")
print(out3)
if err3: print("ERR:", err3)

print("\n=== 4. DF -H ===")
out4, err4 = run_cmd("df -h")
print(out4)
if err4: print("ERR:", err4)

ssh.close()
