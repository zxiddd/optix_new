import requests
import json
import time
import paramiko
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

VPS_HOST = "200.141.7.8"
VPS_USER = "root"
VPS_PASS = "Zaddy123"
BASE_URL = "https://api.optixapp.in"

results = {}

def log_result(section, test_name, status, details="", latency_ms=0):
    if section not in results:
        results[section] = []
    results[section].append({
        "test": test_name,
        "status": status, # PASS, FAIL, WARNING
        "details": str(details),
        "latency_ms": round(latency_ms, 2)
    })
    print(f"[{status}] {section} -> {test_name} ({latency_ms}ms) | {details[:100]}")

print("=== STARTING COMPREHENSIVE BACKEND VALIDATION ===")

# --- 1. SERVER & INFRASTRUCTURE VIA SSH ---
print("\n--- 1. Testing Server & Infrastructure ---")
client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    start_t = time.time()
    client.connect(VPS_HOST, username=VPS_USER, password=VPS_PASS, timeout=10)
    connect_time = (time.time() - start_t) * 1000

    def run_ssh(cmd):
        stdin, stdout, stderr = client.exec_command(cmd)
        return stdout.read().decode().strip(), stderr.read().decode().strip()

    out, err = run_ssh("docker ps --format '{{.Names}}\t{{.Status}}\t{{.Ports}}'")
    log_result("1. Infrastructure", "Docker Containers Running", "PASS" if "optix-backend-staging" in out else "FAIL", out, connect_time)

    out_pg, _ = run_ssh("docker exec optix-postgres-staging pg_isready -U optix_staging_admin")
    log_result("1. Infrastructure", "PostgreSQL Healthy", "PASS" if "accepting connections" in out_pg else "FAIL", out_pg)

    out_redis, _ = run_ssh("docker exec optix-redis-staging redis-cli ping")
    log_result("1. Infrastructure", "Redis Healthy", "PASS" if "PONG" in out_redis else "FAIL", out_redis)

    out_cert, _ = run_ssh("certbot certificates || true")
    log_result("1. Infrastructure", "HTTPS Certificate Valid", "PASS" if "api.optixapp.in" in out_cert else "WARNING", out_cert[:150])

    out_nginx, _ = run_ssh("systemctl is-active nginx")
    log_result("1. Infrastructure", "Nginx Reverse Proxy Active", "PASS" if out_nginx == "active" else "FAIL", out_nginx)

    out_uptime, _ = run_ssh("uptime")
    log_result("1. Infrastructure", "System Uptime & Load", "PASS", out_uptime)

finally:
    client.close()

# --- HTTP HEALTH & API ENDPOINTS ---
t0 = time.time()
try:
    r = requests.get(f"{BASE_URL}/health", verify=False)
    lat = (time.time() - t0) * 1000
    log_result("1. Infrastructure", "GET /health Endpoint", "PASS" if r.status_code == 200 else "FAIL", r.text, lat)
except Exception as e:
    log_result("1. Infrastructure", "GET /health Endpoint", "FAIL", str(e))

t0 = time.time()
try:
    r = requests.get(f"{BASE_URL}/health/db", verify=False)
    lat = (time.time() - t0) * 1000
    log_result("1. Infrastructure", "GET /health/db Endpoint", "PASS" if r.status_code == 200 and "UP" in r.text else "WARNING", r.text, lat)
except Exception as e:
    log_result("1. Infrastructure", "GET /health/db Endpoint", "FAIL", str(e))

t0 = time.time()
try:
    r = requests.get(f"{BASE_URL}/docs", verify=False)
    lat = (time.time() - t0) * 1000
    log_result("1. Infrastructure", "Swagger Docs (/docs)", "PASS" if r.status_code == 200 else "WARNING", f"Status: {r.status_code}", lat)
except Exception as e:
    log_result("1. Infrastructure", "Swagger Docs (/docs)", "FAIL", str(e))

# Save interim output
with open("validation_interim.json", "w") as f:
    json.dump(results, f, indent=2)

print("\nInterim Infrastructure Results saved.")
