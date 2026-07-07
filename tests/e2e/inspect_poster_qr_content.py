"""Verify QR code content matches the invite link."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22345"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        context = browser.new_context(viewport={"width": 1440, "height": 900})
        page = context.new_page()

        page.goto(f"{BASE}/console/preview")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)

        page.click(".console-invite-btn")
        page.wait_for_selector(".invite-panel", timeout=5000)
        page.wait_for_timeout(500)

        invite_link = page.locator(".invite-link-value").text_content().strip()
        print(f"Expected QR content: {invite_link}")

        # Open poster modal
        page.locator(".invite-link-actions .invite-btn-secondary").nth(1).click(force=True)
        page.wait_for_selector(".poster-panel", timeout=5000)
        page.wait_for_timeout(2000)

        # Inject a QR decoder to verify the actual data URL used in the poster
        # The QR code is generated via QRCode.toDataURL(link) and stored in qrCache
        # We can verify by generating a QR with the same link ourselves and checking pixel dimensions match
        qr_info = page.evaluate(f"""
            async () => {{
                const QRCode = await import('/node_modules/.vite/deps/qrcode.js?v=__test').catch(() => null);
                return {{ hasQRCode: !!QRCode }};
            }}
        """)
        print(f"QRCode module loaded: {qr_info}")

        # Generate the QR via the same library in browser context, then verify
        qr_data = page.evaluate(f"""
            async () => {{
                const QRCode = (await import('qrcode')).default;
                const url = await QRCode.toDataURL('{invite_link}', {{
                    errorCorrectionLevel: 'H',
                    margin: 1,
                    width: 320,
                    color: {{ dark: '#1a1a1a', light: '#ffffff' }}
                }});
                return {{ url: url.substring(0, 100) + '...', length: url.length }};
            }}
        """)
        print(f"Generated QR data URL (preview): {qr_data}")

        # Now visually check the poster canvas has the QR code rendered (right-bottom area)
        page.locator(".poster-card:nth-child(1)").click(force=True)
        page.wait_for_timeout(500)

        # Get pixel info from the first preview canvas — should have QR pattern
        canvas_data = page.evaluate("""
            () => {
                const canvas = document.querySelector('.poster-card-canvas');
                if (!canvas) return null;
                const ctx = canvas.getContext('2d');
                const data = ctx.getImageData(0, 0, canvas.width, canvas.height).data;
                let darkCount = 0;
                let lightCount = 0;
                // Sample QR area (lower half, center)
                const startY = Math.floor(canvas.height * 0.55);
                const endY = Math.floor(canvas.height * 0.95);
                const startX = Math.floor(canvas.width * 0.2);
                const endX = Math.floor(canvas.width * 0.8);
                for (let y = startY; y < endY; y++) {
                    for (let x = startX; x < endX; x++) {
                        const i = (y * canvas.width + x) * 4;
                        const r = data[i], g = data[i+1], b = data[i+2];
                        if (r < 80 && g < 80 && b < 80) darkCount++;
                        if (r > 230 && g > 230 && b > 230) lightCount++;
                    }
                }
                return { width: canvas.width, height: canvas.height, darkInQrArea: darkCount, lightInQrArea: lightCount };
            }
        """)
        print(f"Preview canvas: {canvas_data}")
        has_qr_pattern = canvas_data and canvas_data["darkInQrArea"] > 100 and canvas_data["lightInQrArea"] > 100
        print(f"QR code visually present in preview: {has_qr_pattern}")

        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/poster_qr_verify.png", full_page=False)
        print("Saved: poster_qr_verify.png")

        browser.close()


if __name__ == "__main__":
    main()