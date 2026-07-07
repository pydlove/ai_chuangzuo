# Dev SSL Truststore

本目录存放**开发环境**的 JVM 信任库（truststore），文件 `admin-api-truststore.p12` 不入 Git，逐机生成。

## 用途

部分国内平台（如 `*.baidu.com`、`*.weibo.com`）的 HTTPS 证书由自有私有 CA（DLP CA）签发，
标准 JDK cacerts 不信任这些证书，导致 jsoup / HttpClient 在抓取公开页面时抛 `PKIX path building failed`。

线上 Linux JDK 的 cacerts 通常已包含国内 CA 列表，**生产环境不需要这里的 truststore**。

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

`scripts/local/admin-full-stack/start.sh` 检测到本文件存在时会自动通过 `-Djavax.net.ssl.trustStore=...` 注入到 mvn spring-boot:run。
无需额外操作。

## 平台根因表（不要再来回试）

| 平台 | 修了 truststore 后 | 真因 |
|---|---|---|
| baidu | ✅ 50 条数据 | — |
| weibo | ❌ PKIX 通过但 HTML 是"Sina Visitor System"反爬墙 | 需 JS 引擎才能拿到热搜表 |
| bilibili | ❌ HTML 仅 ~4KB，只剩 footer | JS 渲染 SPA，jsoup 拿不到 |
| toutiao | ❌ URL 404 + `Content-Type: text/plain` | URL 已弃用，需找新接口 |
| douyin | ❌ URL 404 + JS 渲染 | jsoup 无法处理 |
