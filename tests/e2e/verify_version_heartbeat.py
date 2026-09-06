#!/usr/bin/env python3
"""验证版本心跳的两种模式：
1. 新版本：version.json 版本号变化 → 底部出现「发现新版本，点击刷新」提示
2. 升级中：version.json 连续拉取失败 → 阻断弹框「系统升级中」，恢复后自动消失
"""
import json
import subprocess
import sys
import time
from pathlib import Path

from playwright.sync_api import sync_playwright

DIST = Path(__file__).resolve().parent.parent.parent / "project/user/web/dist"
SHOTS = Path(__file__).resolve().parent / "screenshots"
SHOTS.mkdir(exist_ok=True)
PORT = 8899
BASE = f"http://localhost:{PORT}"


def read_version():
    return json.loads((DIST / "version.json").read_text())["version"]


def write_version(v):
    (DIST / "version.json").write_text(json.dumps({"version": v, "buildTime": "2026-09-06T00:00:00.000Z"}))


def main():
    page_version = read_version()
    server = subprocess.Popen(
        [sys.executable, "-m", "http.server", str(PORT), "--bind", "127.0.0.1"],
        cwd=DIST, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    failed = False
    try:
        with sync_playwright() as p:
            browser = p.chromium.launch()
            page = browser.new_page(viewport={"width": 1280, "height": 800})
            page.goto(BASE + "/", wait_until="networkidle")

            def visible(sel):
                return page.locator(sel).first.is_visible()

            # 1. 正常状态：无任何提示
            assert not visible("#vhb-upgrading-modal"), "正常状态不应出现升级弹框"
            assert not visible("#vhb-new-version-toast"), "正常状态不应出现新版本提示"
            print("PASS 正常状态无提示")

            # 2. 新版本：篡改 version.json 后触发一次检查
            write_version("build-9999999999999")
            page.evaluate("document.dispatchEvent(new Event('visibilitychange'))")
            page.wait_for_selector("#vhb-new-version-toast", timeout=10000)
            assert visible("#vhb-new-version-toast")
            page.screenshot(path=str(SHOTS / "version_heartbeat_new_version.png"))
            print("PASS 新版本提示出现（截图 version_heartbeat_new_version.png）")
            browser.close()

            # 3. 升级中：拦截 version.json 请求，连续失败两次后应弹框
            browser = p.chromium.launch()
            page = browser.new_page(viewport={"width": 1280, "height": 800})

            def abort_version(route):
                if "version.json" in route.request.url:
                    route.abort()
                else:
                    route.continue_()

            page.route(f"{BASE}/**", abort_version)
            page.goto(BASE + "/", wait_until="networkidle")
            page_version_now = read_version()  # 此时文件被拦截，读的是磁盘值（= page_version）
            page.evaluate("document.dispatchEvent(new Event('visibilitychange'))")  # 第 1 次失败
            time.sleep(2)
            assert not visible("#vhb-upgrading-modal"), "失败 1 次不应弹框"
            print("PASS 单次失败不误报")
            page.wait_for_selector("#vhb-upgrading-modal", timeout=130000)  # 等第 2 次心跳失败
            assert visible("#vhb-upgrading-modal")
            page.screenshot(path=str(SHOTS / "version_heartbeat_upgrading.png"))
            print("PASS 升级中弹框出现（截图 version_heartbeat_upgrading.png）")

            # 4. 恢复：放行请求且版本与页面一致 → 弹框自动消失（不刷新）
            page.unroute_all()
            write_version(page_version)
            page.evaluate("document.dispatchEvent(new Event('visibilitychange'))")
            page.wait_for_timeout(3000)
            assert not visible("#vhb-upgrading-modal"), "服务恢复后弹框应自动消失"
            print("PASS 恢复后弹框自动消失")
            browser.close()
    except AssertionError as e:
        print(f"FAIL {e}")
        failed = True
    finally:
        server.terminate()
        write_version(page_version)
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
