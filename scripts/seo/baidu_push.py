#!/usr/bin/env python3
"""
百度普通收录 API 推送脚本
用法:
    export BAIDU_SITE=https://www.ichuang.top
    export BAIDU_TOKEN=你的token
    python3 scripts/seo/baidu_push.py scripts/seo/urls.txt

也可以命令行传参:
    python3 scripts/seo/baidu_push.py scripts/seo/urls.txt --site https://www.ichuang.top --token 你的token
"""
import argparse
import os
import sys
import urllib.error
import urllib.parse
import urllib.request


def push_urls(site: str, token: str, urls: list[str]) -> dict:
    # site 直接作为 query 参数，不二次编码
    params = urllib.parse.urlencode({"site": site, "token": token})
    endpoint = f"http://data.zz.baidu.com/urls?{params}"
    # 百度要求每行一个 URL，末尾保留换行
    data = ("\n".join(urls) + "\n").encode("utf-8")

    req = urllib.request.Request(
        endpoint,
        data=data,
        headers={"Content-Type": "text/plain; charset=utf-8", "Content-Length": str(len(data))},
        method="POST",
    )

    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return {"status": resp.status, "body": resp.read().decode("utf-8")}
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8") if e.fp else ""
        raise RuntimeError(f"HTTP {e.code}: {body or e.reason}") from e


def main():
    parser = argparse.ArgumentParser(description="推送 URL 到百度普通收录 API")
    parser.add_argument("urls_file", help="每行一个 URL 的文本文件")
    parser.add_argument("--site", default=os.getenv("BAIDU_SITE"), help="在百度站长平台验证的站点域名")
    parser.add_argument("--token", default=os.getenv("BAIDU_TOKEN"), help="百度推送准入密钥")
    args = parser.parse_args()

    if not args.site or not args.token:
        print("错误: 请通过环境变量 BAIDU_SITE / BAIDU_TOKEN 或 --site / --token 提供站点和 token", file=sys.stderr)
        sys.exit(1)

    with open(args.urls_file, "r", encoding="utf-8") as f:
        urls = [line.strip() for line in f if line.strip() and not line.startswith("#")]

    if not urls:
        print("没有要推送的 URL")
        sys.exit(0)

    print(f"即将推送 {len(urls)} 个 URL 到 {args.site} ...")
    try:
        result = push_urls(args.site, args.token, urls)
        print(f"HTTP {result['status']}")
        print(result["body"])
    except Exception as e:
        print(f"推送失败: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
