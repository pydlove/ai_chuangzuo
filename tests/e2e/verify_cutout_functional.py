from playwright.sync_api import sync_playwright
BASE = 'http://localhost:28586'

JS_GENERATE_IMAGE = """
() => {
  const canvas = document.createElement('canvas');
  canvas.width = 200;
  canvas.height = 200;
  const ctx = canvas.getContext('2d');
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, 200, 200);
  ctx.beginPath();
  ctx.arc(100, 100, 60, 0, Math.PI * 2);
  ctx.fillStyle = '#ff2442';
  ctx.fill();
  return canvas.toDataURL('image/png');
}
"""

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 900})

        page.goto(f'{BASE}/tools/cutout')
        page.wait_for_load_state('networkidle')

        # 在页面内生成测试图并通过 dataTransfer 触发 drop
        data_url = page.evaluate(JS_GENERATE_IMAGE)
        page.evaluate(
            """(dataUrl) => {
                const canvas = document.createElement('canvas');
                canvas.width = 200; canvas.height = 200;
                const ctx = canvas.getContext('2d');
                const img = new Image();
                img.onload = () => {
                    ctx.drawImage(img, 0, 0);
                    canvas.toBlob(blob => {
                        const file = new File([blob], 'test.png', { type: 'image/png' });
                        const dt = new DataTransfer();
                        dt.items.add(file);
                        const event = new DragEvent('drop', { dataTransfer: dt, bubbles: true });
                        document.querySelector('.cutout-canvas-wrap').dispatchEvent(event);
                    });
                };
                img.src = dataUrl;
            }""",
            data_url
        )

        # 等待原图 canvas 出现
        page.wait_for_selector('.cutout-canvas', state='visible', timeout=10000)
        page.wait_for_timeout(500)

        # 点击抠图
        page.locator('button:has-text("抠图")').first.click()
        page.wait_for_timeout(800)

        # 检查结果 canvas 是否出现且含有透明像素
        has_transparency = page.evaluate(
            """() => {
                const canvases = document.querySelectorAll('.cutout-canvas');
                if (canvases.length < 2) return false;
                const ctx = canvases[1].getContext('2d');
                const data = ctx.getImageData(0, 0, canvases[1].width, canvases[1].height).data;
                for (let i = 3; i < data.length; i += 4) {
                    if (data[i] < 255) return true;
                }
                return false;
            }"""
        )
        assert has_transparency, '结果图应包含透明像素'
        print('cutout functional OK')

        # 检查下载按钮可用
        with page.expect_download():
            page.locator('button:has-text("下载")').first.click()
        print('download OK')

        page.screenshot(path='/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/cutout_result.png')
        browser.close()

if __name__ == '__main__':
    main()
