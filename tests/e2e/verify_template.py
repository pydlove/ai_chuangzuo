from playwright.sync_api import sync_playwright

errors = []

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={'width': 1280, 'height': 900})

    def on_console(msg):
        if msg.type == 'error':
            errors.append(msg.text)

    page.on('console', on_console)

    page.goto('http://localhost:8080/.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html')
    page.wait_for_load_state('networkidle')

    # Call openTemplateLibrary directly
    result = page.evaluate("() => { try { openTemplateLibrary(); return 'success'; } catch(e) { return 'error: ' + e.message; } }")
    print(f"openTemplateLibrary result: {result}")
    page.wait_for_timeout(1500)

    # Screenshot to see the modal
    page.screenshot(path='/tmp/verify_state.png', full_page=True)

    # Check if modal exists
    modal = page.locator('#template-lib-modal')
    modal_count = modal.count()
    print(f"Modal #template-lib-modal found: {modal_count}")

    if modal_count > 0:
        modal_visible = modal.first.is_visible()
        print(f"Modal visible: {modal_visible}")

        # Check the sub text
        try:
            sub_text = page.locator('text=共').first
            print(f"Sub text visible: {sub_text.is_visible()}, content: {sub_text.inner_text()[:100] if sub_text.is_visible() else 'N/A'}")
        except Exception as e:
            print(f"Could not find sub text: {e}")

        # Check for platform tabs
        tabs = ['全部', '公众号', '小红书', '今日头条', '百家号', '知乎', '抖音图文', '通用风格']
        for tab in tabs:
            try:
                t = page.locator(f'text={tab}').first
                if t.is_visible():
                    print(f"  Tab '{tab}' visible: true")
            except:
                pass

        # Try to count template cards
        try:
            # Look for the template list items
            items = page.locator('[id="template-lib-modal"] [style*="cursor: pointer"]').all()
            print(f"Template cards (cursor:pointer): {len(items)}")
        except:
            pass

        # Check template count in sub text
        try:
            sub = page.evaluate("() => { var m = document.getElementById('template-lib-modal'); if (!m) return null; var subs = m.querySelectorAll('div'); for (var s of subs) { if (s.textContent.includes('共') && s.textContent.includes('模板')) return s.textContent; } return null; }")
            print(f"Sub text content: {sub}")
        except Exception as e:
            print(f"Could not get sub: {e}")

    print(f"\nConsole errors: {len(errors)}")
    for e in errors:
        print(f"  ERROR: {e}")

    browser.close()

print("\nVerification complete")
