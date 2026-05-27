import os
import re
import requests
import sys

DIR = r"D:\成都天奥信息科技有限公司\GJB\word"
TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwidXNlcm5hbWUiOiJhZG1pbiIsImlhdCI6MTc3OTg3NjM3NSwiZXhwIjoxNzc5OTYyNzc1fQ.7Yj_hcCTYOXzD-RSa2A6MjKESyrRDAn8F_kBBgdiWPE"
URL = "http://localhost:8080/api/v1/standards/batch-upload"

# Get all .docx files
all_files = [f for f in os.listdir(DIR) if f.endswith('.docx') and f != 'GJB.docx' and f != 'GJB-已标记.docx']

# Parse standard code from filename
def parse_code(filename):
    # Match patterns like: GJB 3206B-2022, GJB-Z 170.1-2013, GJB 150.10A-2009
    # Also: GJBZ 768A-1998 (no space), GJB_Z 1391-2006 (underscore)
    name = filename.replace('.docx', '').replace('《', ' ').replace('》', ' ')
    # Normalize: GJBZ -> GJB Z, GJB_Z -> GJB-Z
    name = re.sub(r'\bGJBZ\b', 'GJB-Z', name)
    name = re.sub(r'\bGJB_Z\b', 'GJB-Z', name)
    name = re.sub(r'\bGJB[- ]?Z\b', 'GJB-Z', name)

    m = re.match(r'(GJB[A-Z]?[- ]?(?:Z\s*)?\d+[A-Za-z]*(?:\.\d+)?[A-Za-z]?[-－]\d{2,4})', name)
    if m:
        code = m.group(1)
        code = code.replace(' ', '').replace('－', '-')
        return code
    # Try GBT pattern
    m = re.match(r'(GBT?\s?\d+[A-Za-z]*(?:\.\d+)?[A-Za-z]?[-－]\d{2,4})', name)
    if m:
        return m.group(1).replace(' ', '').replace('－', '-')
    return None

# Deduplicate by standard code - keep best filename (prefer shorter, cleaner names)
code_map = {}  # code -> (filename, name_hint)
no_code = []

for f in sorted(all_files):
    code = parse_code(f)
    if code:
        if code not in code_map or len(f) < len(code_map[code]):
            code_map[code] = f
    else:
        no_code.append(f)

print(f"Total .docx files: {len(all_files)}")
print(f"Unique standards by code: {len(code_map)}")
print(f"Files without parseable code: {len(no_code)}")

# Skip GBT (non-military)
gjb_codes = {k: v for k, v in code_map.items() if k.startswith('GJB')}
gbt_codes = {k: v for k, v in code_map.items() if not k.startswith('GJB')}
print(f"GJB standards: {len(gjb_codes)}")
print(f"GBT/non-military: {len(gbt_codes)}")

# Upload GJB standards in batches
upload_files = list(gjb_codes.values())
batch_size = 5
total = len(upload_files)
success = 0
failed = []

for i in range(0, total, batch_size):
    batch = upload_files[i:i + batch_size]
    files = []
    for f in batch:
        path = os.path.join(DIR, f)
        files.append(('files', (f, open(path, 'rb'), 'application/vnd.openxmlformats-officedocument.wordprocessingml.document')))

    try:
        resp = requests.post(URL, headers={'Authorization': f'Bearer {TOKEN}'}, files=files, timeout=300)
        if resp.status_code == 200:
            data = resp.json()
            count = len(data.get('data', []))
            success += count
            codes = [parse_code(f) for f in batch]
            print(f"[{min(i+batch_size,total)}/{total}] OK: {', '.join(codes)}")
        else:
            print(f"[{min(i+batch_size,total)}/{total}] FAIL {resp.status_code}: {resp.text[:200]}")
            failed.extend(batch)
    except Exception as e:
        print(f"[{min(i+batch_size,total)}/{total}] ERROR: {e}")
        failed.extend(batch)

    # Close files
    for _, (_, fh, _) in files:
        fh.close()

print(f"\nDone. Success: {success}, Failed: {len(failed)}")
if failed:
    print("Failed files:")
    for f in failed:
        print(f"  {parse_code(f)}: {f}")

# Print unparseable files
if no_code:
    print(f"\nSkipped {len(no_code)} files (could not parse standard code):")
    for f in sorted(no_code)[:20]:
        print(f"  {f}")
    if len(no_code) > 20:
        print(f"  ... and {len(no_code)-20} more")
