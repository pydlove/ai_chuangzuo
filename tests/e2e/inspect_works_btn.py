from playwright.sync_api import sync_playwright

BASE = "http://localhost:4173"

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1280, "height": 800})

        # Seed a completed work
        page.goto(BASE + "/blank")
        page.evaluate("""() => {
            const works = [{
                id: 'test_work_1',
                title: '测试已生成作品',
                status: 'completed',
                platform: '微信公众号',
                wordCount: 1500,
                style: '职场干货',
                template: '默认模板',
                completedAt: new Date().toISOString(),
                content: '测试内容'
            }];
            localStorage.setItem('aichuangzuo_generation_queue', JSON.stringify(works));
        }""")

        page.goto(BASE + "/console/works")
        page.wait_for_timeout(1500)

        # Find the primary button and get its computed box-shadow
        shadow = page.eval_on_selector(".primary-btn", "el => getComputedStyle(el).boxShadow")
        print(f"Primary button box-shadow: {shadow}")

        # Hover and check
        page.hover(".primary-btn")
        page.wait_for_timeout(300)
        hover_shadow = page.eval_on_selector(".primary-btn", "el => getComputedStyle(el).boxShadow")
        print(f"Primary button hover box-shadow: {hover_shadow}")

        # Also get the ant-btn inner span shadow if any
        inner_shadow = page.eval_on_selector(".primary-btn > span", "el => getComputedStyle(el).boxShadow")
        print(f"Primary button inner span box-shadow: {inner_shadow}")

        # Take screenshot
        page.screenshot(path="tests/e2e/screenshots/works_primary_btn.png", full_page=False)
        print("Saved screenshot tests/e2e/screenshots/works_primary_btn.png")

        browser.close()

if __name__ == "__main__":
    main()
