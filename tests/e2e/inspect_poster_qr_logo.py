"""Verify posters have logo + scannable QR code via canvas inspection."""
import base64
from pathlib import Path
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22345"
DOWNLOADS = Path("/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/downloads")
SCREENSHOTS = Path("/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots")


def main():
    DOWNLOADS.mkdir(parents=True, exist_ok=True)
    for f in DOWNLOADS.glob("*"):
        f.unlink()

    with sync_playwright() as p:
        browser = p.chromium.launch()
        context = browser.new_context(viewport={"width": 1440, "height": 900}, accept_downloads=True)
        page = context.new_page()

        page.goto(f"{BASE}/console/preview")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)

        page.click(".console-invite-btn")
        page.wait_for_selector(".invite-panel", timeout=5000)
        page.wait_for_timeout(500)

        invite_link = page.locator(".invite-link-value").text_content().strip()
        invite_code = page.locator(".invite-code-value").text_content().strip()
        print(f"invite link: {invite_link}")
        print(f"invite code: {invite_code}")

        # Capture poster modal screenshot first
        page.locator(".invite-link-actions .invite-btn-secondary").nth(1).click(force=True)
        page.wait_for_selector(".poster-panel", timeout=5000)
        page.wait_for_timeout(2500)
        page.screenshot(path=str(SCREENSHOTS / "poster_modal_with_qr.png"), full_page=False)
        print("Saved: poster_modal_with_qr.png")

        # Download all 4 templates in sequence
        templates = ["classic-red", "clean-white", "dark-premium", "fresh-green"]
        downloaded = []

        for i, tpl_id in enumerate(templates):
            # Use force=True and bypass any overlay issues from async rendering
            page.locator(f".poster-card:nth-child({i + 1})").click(force=True)
            page.wait_for_timeout(500)

            with page.expect_download(timeout=8000) as download_info:
                page.locator(".poster-actions .invite-btn-primary").click(force=True)
            dl = download_info.value
            target = DOWNLOADS / dl.suggested_filename
            dl.save_as(str(target))
            print(f"Downloaded: {dl.suggested_filename} ({target.stat().st_size} bytes)")
            downloaded.append({"template": tpl_id, "file": target, "size": target.stat().st_size})

            # Re-open modal for next (skip after last)
            if tpl_id != templates[-1]:
                page.wait_for_timeout(500)
                page.locator(".invite-link-actions .invite-btn-secondary").nth(1).click(force=True)
                page.wait_for_selector(".poster-panel", timeout=5000)
                page.wait_for_timeout(2000)

        browser.close()

    # Inspect each downloaded PNG in a fresh browser session
    print("\n=== Inspecting downloaded posters ===")
    with sync_playwright() as p:
        browser = p.chromium.launch()
        context = browser.new_context(viewport={"width": 800, "height": 800})
        all_pass = True
        for r in downloaded:
            page = context.new_page()
            png_data = r["file"].read_bytes()
            b64 = base64.b64encode(png_data).decode()
            page.set_content(f"""
                <html><body>
                <img id="poster" src="data:image/png;base64,{b64}" style="width:400px" />
                <canvas id="qc" width="400" height="500" style="display:none"></canvas>
                </body></html>
            """)
            page.wait_for_timeout(500)

            stats = page.evaluate("""
                async () => {
                    const img = document.getElementById('poster');
                    const c = document.getElementById('qc');
                    const ctx = c.getContext('2d');
                    ctx.drawImage(img, 0, 0, c.width, c.height);
                    const data = ctx.getImageData(0, 0, c.width, c.height).data;
                    let dark = 0, light = 0, colored = 0;
                    let reds = 0;
                    for (let i = 0; i < data.length; i += 4) {
                        const r = data[i], g = data[i+1], b = data[i+2];
                        const isGray = Math.abs(r-g) < 15 && Math.abs(g-b) < 15 && Math.abs(r-b) < 15;
                        if (isGray && (r + g + b) < 200) dark++;
                        else if (isGray && (r + g + b) > 600) light++;
                        else if (!isGray) {
                            colored++;
                            // Logo is red/pink
                            if (r > 200 && g < 150 && b < 150) reds++;
                        }
                    }
                    return { dark, light, colored, reds };
                }
            """)
            has_qr = stats["dark"] > 3000 and stats["light"] > 3000
            has_logo = stats["reds"] > 500  # logo is red

            status = "PASS" if (has_qr and has_logo) else "FAIL"
            print(f"{status}: {r['template']} — {r['file'].name} ({r['size']} bytes)")
            print(f"        dark={stats['dark']}, light={stats['light']}, colored={stats['colored']}, reds={stats['reds']}")
            if status == "FAIL":
                all_pass = False
            page.close()
        browser.close()

    print(f"\nAll posters have QR + logo: {all_pass}")


if __name__ == "__main__":
    main()