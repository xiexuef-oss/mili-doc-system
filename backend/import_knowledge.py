"""Import GJB text files into the knowledge base."""
import requests
import os
import sys

BASE = "http://localhost:8080/api/v1"
# 默认凭据 — 可通过环境变量覆盖
ADMIN_USER = os.environ.get("MILIDOC_USER", "admin")
ADMIN_PASS = os.environ.get("MILIDOC_PASS", "admin123")

def login():
    """获取 JWT token"""
    print(f"Logging in as {ADMIN_USER}...")
    resp = requests.post(f"{BASE}/auth/login", json={
        "username": ADMIN_USER,
        "password": ADMIN_PASS
    })
    if resp.status_code != 200:
        print(f"Login FAILED ({resp.status_code}): {resp.text}")
        sys.exit(1)
    data = resp.json()
    token = data.get("data", {}).get("token") or data.get("token")
    if not token:
        print(f"No token in response: {data}")
        sys.exit(1)
    print("Login OK")
    return token

TOKEN = login()
HEADERS = {
    "Authorization": f"Bearer {TOKEN}",
    "Content-Type": "application/json"
}

FILES = [
    ("D:/成都天奥信息科技有限公司/GJB/军工产品研制/军工产品研制技术审查评审指南.txt", "军工产品研制技术审查评审指南", "GJB 技术审查评审指南"),
    ("D:/成都天奥信息科技有限公司/GJB/军工产品研制/军工产品研制技术文件编写范例.txt", "军工产品研制技术文件编写范例", "GJB 编写范例 范文"),
    ("D:/成都天奥信息科技有限公司/GJB/军工产品研制/军工产品研制技术文件编写说明.txt", "军工产品研制技术文件编写说明", "GJB 编写说明 编写要求"),
    ("D:/成都天奥信息科技有限公司/GJB/军工产品研制/军工产品研制技术文件编写指南.txt", "军工产品研制技术文件编写指南", "GJB 5882 编写指南"),
]

success_count = 0
fail_count = 0

for filepath, title, tags in FILES:
    filename = os.path.basename(filepath)
    print(f"\nReading {filename}...")
    if not os.path.exists(filepath):
        print(f"  SKIP: file not found")
        fail_count += 1
        continue

    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    body = {
        "title": title,
        "content": content,
        "category": "GJB",
        "tags": tags,
        "status": "ACTIVE"
    }

    print(f"  Uploading ({len(content)} chars)...")
    resp = requests.post(f"{BASE}/knowledge", json=body, headers=HEADERS)
    if resp.status_code == 200:
        data = resp.json()
        print(f"  OK -> id={data.get('data', {}).get('id', '?')}")
        success_count += 1
    else:
        print(f"  FAIL ({resp.status_code}): {resp.text[:200]}")
        fail_count += 1

print(f"\nDone. Success: {success_count}, Failed: {fail_count}")
