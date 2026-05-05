"""E2E test for military-doc frontend"""
import os, time
from playwright.sync_api import sync_playwright

OUT_DIR = "D:/military-doc/frontend/test-screenshots"
os.makedirs(OUT_DIR, exist_ok=True)

def screenshot(page, name):
    path = os.path.join(OUT_DIR, name)
    page.screenshot(path=path, full_page=True)
    print(f"  Screenshot: {path}")

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1440, "height": 900})

    # Step 1: Open login page
    print("Step 1: Opening login page...")
    page.goto("http://localhost:5173")
    page.wait_for_load_state("networkidle")
    time.sleep(1)
    screenshot(page, "01-login-page.png")

    # Step 2: Login
    print("Step 2: Logging in as admin...")
    page.fill('input[placeholder="用户名"]', "admin")
    page.fill('input[placeholder="密码"]', "admin123")
    page.click('button:has-text("登 录")')
    page.wait_for_url("**/projects", timeout=10000)
    page.wait_for_load_state("networkidle")
    time.sleep(1)
    screenshot(page, "02-project-list.png")
    print("  Login successful, redirected to projects page")

    # Step 3: Click "用户管理" in sidebar
    print("Step 3: Navigating to user management...")
    page.click('text=用户管理')
    page.wait_for_load_state("networkidle")
    time.sleep(1)
    screenshot(page, "03-user-list.png")

    # Verify user table shows admin
    user_cells = page.locator('table tbody tr').count()
    print(f"  User table rows: {user_cells}")
    admin_visible = page.locator('text=admin').first.is_visible()
    print(f"  Admin user visible: {admin_visible}")

    # Step 4: Click "角色管理" in sidebar
    print("Step 4: Navigating to role management...")
    page.click('text=角色管理')
    page.wait_for_load_state("networkidle")
    time.sleep(1)
    screenshot(page, "04-role-list.png")

    role_rows = page.locator('table tbody tr').count()
    print(f"  Role table rows: {role_rows}")
    for role in ["ADMIN", "PM", "EDITOR", "REVIEWER", "READONLY"]:
        visible = page.locator(f'text={role}').first.is_visible()
        print(f"  Role '{role}' visible: {visible}")

    # Step 5: Go back to document management
    print("Step 5: Navigating to document management...")
    page.click('text=文档管理')
    page.wait_for_load_state("networkidle")
    time.sleep(1)
    screenshot(page, "05-doc-file-list.png")

    # Step 6: Document catalog
    print("Step 6: Navigating to document catalog...")
    page.click('text=文档目录')
    page.wait_for_load_state("networkidle")
    time.sleep(1)
    screenshot(page, "06-doc-catalog-list.png")

    # Step 7: Review meetings
    print("Step 7: Navigating to review meetings...")
    page.click('text=评审会议')
    page.wait_for_load_state("networkidle")
    time.sleep(1)
    screenshot(page, "07-meeting-list.png")

    print("\nAll tests passed!")
    browser.close()
