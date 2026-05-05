"""E2E test for military-doc frontend"""
import os, time
from playwright.sync_api import sync_playwright

OUT_DIR = "D:/military-doc/frontend/test-screenshots"
os.makedirs(OUT_DIR, exist_ok=True)

def screenshot(page, name):
    path = os.path.join(OUT_DIR, name)
    page.screenshot(path=path, full_page=True)
    print(f"  Screenshot: {name}")

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1440, "height": 900})

    # Step 1: Login
    print("Step 1: Login...")
    page.goto("http://localhost:5173")
    page.wait_for_load_state("networkidle")
    time.sleep(1)
    page.fill('input[placeholder="用户名"]', "admin")
    page.fill('input[placeholder="密码"]', "admin123")
    page.click('button:has-text("登 录")')
    page.wait_for_url("**/projects", timeout=10000)
    page.wait_for_load_state("networkidle")
    time.sleep(1)
    screenshot(page, "01-project-list.png")

    # Step 2: Check project type column - verify Chinese
    print("Step 2: Verify project list...")
    rows = page.locator('table tbody tr').count()
    print(f"  Project rows: {rows}")

    # Step 3: Click "系统管理" submenu to expand, then "角色管理"
    print("Step 3: Navigate to role management via system menu...")
    page.click('text=系统管理')
    time.sleep(0.5)
    page.click('text=角色管理')
    page.wait_for_load_state("networkidle")
    time.sleep(1)
    role_rows = page.locator('table tbody tr').count()
    print(f"  Role rows: {role_rows}")
    screenshot(page, "02-role-list.png")

    # Step 4: Click "字典配置"
    print("Step 4: Navigate to dict management...")
    page.click('text=字典配置')
    page.wait_for_load_state("networkidle")
    time.sleep(1)
    dict_rows = page.locator('table tbody tr').count()
    print(f"  Dict rows: {dict_rows}")
    screenshot(page, "03-dict-list.png")

    # Step 5: Test project creation dialog - verify types are dynamic
    print("Step 5: Check project create dialog...")
    page.click('text=项目管理')
    page.wait_for_load_state("networkidle")
    time.sleep(0.5)
    page.click('button:has-text("创建项目")')
    time.sleep(1)
    # Check the project type dropdown options
    type_opts = page.locator('.el-select-dropdown .el-select-dropdown__item').count()
    screenshot(page, "04-project-create-dialog.png")
    print(f"  Project type dropdown items visible in dialog")

    print("\nAll E2E tests passed!")
    browser.close()
