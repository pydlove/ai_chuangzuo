# Dev SSL Truststore

本目录存放**开发环境**的 JVM 信任库（truststore），文件 `admin-api-truststore.p12` 不入 Git，逐机生成。

## 用途

部分国内平台（如 `*.baidu.com`、`*.weibo.com`）以及本地 AI 调用（如 `*.minimax.chat`）的 HTTPS 证书会被本地 DLP 代理替换为私有 CA（DLP CA）签发的证书，
标准 JDK cacerts 不信任这些证书，导致 jsoup / HttpClient / RestTemplate 抛 `PKIX path building failed`。

线上 Linux JDK 的 cacerts 通常已包含国内 CA 列表且不存在本地 DLP 拦截，**生产环境不需要这里的 truststore**。

## 重新生成（首次拉代码后）

```bash
TS=config/ssl/admin-api-truststore.p12

# 1. 从 JDK 17 cacerts 导出全部 trusted cert，作为基础
keytool -importkeystore \
  -srckeystore "$JAVA_HOME/lib/security/cacerts" -srcstorepass changeit \
  -destkeystore "$TS" -deststorepass changeit \
  -deststoretype PKCS12 -srcstoretype JKS -noprompt

# 2. 从 baidu 证书链提取 DLP CA 自签根证书
openssl s_client -connect top.baidu.com:443 -servername top.baidu.com -showcerts \
  </dev/null 2>/dev/null \
  | awk '/BEGIN CERTIFICATE/{n++; if(n==2)flag=1} flag; /END CERTIFICATE/{if(flag){flag=0}}' \
  > /tmp/dlp_ca.crt

# 3. 导入到 truststore
keytool -import -trustcacerts -alias dlp_ca_root \
  -file /tmp/dlp_ca.crt \
  -keystore "$TS" -storepass changeit -storetype PKCS12 -noprompt
```

## 启动方式

`scripts/local/admin-full-stack/start.sh` 与 `scripts/local/user-full-stack/start.sh` 检测到本文件存在时会自动通过 `-Djavax.net.ssl.trustStore=...` 注入到 mvn spring-boot:run。
无需额外操作。

## 平台根因表（不要再来回试）

| 平台 | 状态 | 数据源 | 说明 |
|---|---|---|---|
| baidu | ✅ 50 条数据 | jsoup 抓 `top.baidu.com/board` | 需 dev truststore（DLP CA） |
| douyin | ✅ 50 条数据 | jsoup 抓 `aweme/v1/web/hot/search/list` JSON | — |
| toutiao | ✅ 50 条数据 | CDP 抓 `toutiao.com/hot-event/hot-board` JSON | `/trending/` 是 JS 空壳，热榜数据走 JSON 接口 |
| bilibili | ✅ 50 条数据 | CDP 抓 `bilibili.com/v/popular/rank/all` 渲染后 DOM | 原 `ranking/v2` API 需 SESSDATA cookie |
| weibo | ✅ 50 条数据 | CDP 抓 `s.weibo.com/top/summary` 渲染后 DOM | 原页面走 Sina Visitor System 反爬墙 |

> CDP = Chrome DevTools Protocol，由 `ChromeDevToolsFetcher` 启动本地 Chrome 并驱动。见 `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/crawler/`。
