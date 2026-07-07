#!/bin/bash
# ===================================================================
# 爱创作 - 热搜抓取环境初始化脚本
# 运行环境：服务器（Linux）
# 用途：首次启动时自动探测 Chrome 路径、生成 DLP CA truststore，
#       并将相关环境变量写入 /root/app/aichuangzuo/.env
# 特性：幂等，重复执行不会破坏已有配置
# ===================================================================

set -e

APP_DIR="${APP_DIR:-/root/app/aichuangzuo}"
ENV_FILE="${APP_DIR}/.env"
SSL_DIR="${APP_DIR}/config/ssl"
TRUSTSTORE="${SSL_DIR}/admin-api-truststore.p12"
TRUSTSTORE_PASS="changeit"

log_info() { echo -e "\033[0;32m[INFO]\033[0m $1"; }
log_warn() { echo -e "\033[1;33m[WARN]\033[0m $1"; }
log_error() { echo -e "\033[0;31m[ERROR]\033[0m $1"; }

# 确保 .env 文件存在
mkdir -p "$(dirname "$ENV_FILE")"
touch "$ENV_FILE"

# 幂等更新 .env 中的某一行：key=value
# 如果 key 已存在则替换，不存在则追加
update_env() {
    local key="$1"
    local value="$2"
    if grep -q "^${key}=" "$ENV_FILE" 2>/dev/null; then
        # 使用临时文件避免 sed 原地编辑在不同平台不兼容
        sed "s|^${key}=.*|${key}=${value}|" "$ENV_FILE" > "${ENV_FILE}.tmp"
        mv "${ENV_FILE}.tmp" "$ENV_FILE"
    else
        echo "${key}=${value}" >> "$ENV_FILE"
    fi
}

# 1. 自动探测 Chrome 可执行文件
CHROME_PATH=""
for candidate in \
    /usr/bin/google-chrome \
    /usr/bin/google-chrome-stable \
    /usr/bin/chromium \
    /usr/bin/chromium-browser \
    /usr/local/bin/google-chrome \
    /usr/local/bin/chromium \
    /opt/google/chrome/google-chrome \
    /Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome

do
    if [ -x "$candidate" ]; then
        CHROME_PATH="$candidate"
        break
    fi
done

if [ -z "$CHROME_PATH" ]; then
    log_warn "未探测到 Chrome/Chromium 可执行文件，请手动安装后配置 HOT_SEARCH_CHROME_PATH"
    log_warn "常见安装命令："
    log_warn "  CentOS/RHEL: yum install -y google-chrome-stable"
    log_warn "  Ubuntu/Debian: apt-get install -y google-chrome-stable"
else
    log_info "探测到 Chrome: ${CHROME_PATH}"
    update_env "HOT_SEARCH_CHROME_PATH" "$CHROME_PATH"
fi

# 2. 生成 DLP CA truststore（用于 baidu/weibo 等国内自签 CA）
# 仅当 truststore 不存在时生成，避免重复操作
if [ -f "$TRUSTSTORE" ]; then
    log_info "truststore 已存在: ${TRUSTSTORE}，跳过生成"
else
    log_info "首次生成 dev truststore: ${TRUSTSTORE}"
    mkdir -p "$SSL_DIR"

    if ! command -v keytool >/dev/null 2>&1; then
        log_error "未找到 keytool，无法生成 truststore。请安装 JDK 17。"
        exit 1
    fi

    if ! command -v openssl >/dev/null 2>&1; then
        log_error "未找到 openssl，无法生成 truststore。请安装 openssl。"
        exit 1
    fi

    # 尝试从系统 JAVA_HOME 找 cacerts，回退到常见路径
    CACERTS="${JAVA_HOME}/lib/security/cacerts"
    if [ ! -f "$CACERTS" ]; then
        CACERTS="/usr/lib/jvm/java-17-openjdk/lib/security/cacerts"
    fi
    if [ ! -f "$CACERTS" ]; then
        CACERTS="/usr/lib/jvm/java-17/lib/security/cacerts"
    fi

    if [ ! -f "$CACERTS" ]; then
        log_warn "未找到 JDK cacerts，truststore 将只包含 DLP CA（可能影响其他 HTTPS 请求）"
        # 创建一个空的 PKCS12 truststore
        keytool -genkeypair -alias temp -keyalg RSA -keystore "$TRUSTSTORE" \
            -storepass "$TRUSTSTORE_PASS" -storetype PKCS12 -dname "CN=temp" -noprompt >/dev/null 2>&1 || true
        keytool -delete -alias temp -keystore "$TRUSTSTORE" \
            -storepass "$TRUSTSTORE_PASS" -storetype PKCS12 -noprompt >/dev/null 2>&1 || true
    else
        log_info "从 ${CACERTS} 导入系统根证书..."
        keytool -importkeystore \
            -srckeystore "$CACERTS" -srcstorepass changeit \
            -destkeystore "$TRUSTSTORE" -deststorepass "$TRUSTSTORE_PASS" \
            -deststoretype PKCS12 -srcstoretype JKS -noprompt
    fi

    # 提取 DLP CA 自签根证书（baidu/weibo 证书链的第二个证书）
    DLP_CA_FILE="/tmp/dlp_ca.crt"
    log_info "从 top.baidu.com 提取 DLP CA 根证书..."
    if openssl s_client -connect top.baidu.com:443 -servername top.baidu.com -showcerts \
        </dev/null 2>/dev/null | \
        awk '/BEGIN CERTIFICATE/{n++; if(n==2)flag=1} flag; /END CERTIFICATE/{if(flag){flag=0}}' > "$DLP_CA_FILE" && \
        [ -s "$DLP_CA_FILE" ]; then
        keytool -import -trustcacerts -alias dlp_ca_root \
            -file "$DLP_CA_FILE" \
            -keystore "$TRUSTSTORE" -storepass "$TRUSTSTORE_PASS" -storetype PKCS12 -noprompt
        rm -f "$DLP_CA_FILE"
        log_info "DLP CA 已导入 truststore"
    else
        log_warn "未能从 top.baidu.com 提取 DLP CA，truststore 可能无法访问 baidu/weibo"
    fi
fi

# 3. 将 truststore 注入 JVM 环境变量
# 使用 JAVA_TOOL_OPTIONS，JVM 会自动读取，无需修改 ExecStart
TRUSTSTORE_OPTS="-Djavax.net.ssl.trustStore=${TRUSTSTORE} -Djavax.net.ssl.trustStorePassword=${TRUSTSTORE_PASS} -Djavax.net.ssl.trustStoreType=PKCS12"
update_env "JAVA_TOOL_OPTIONS" "$TRUSTSTORE_OPTS"

log_info "热搜抓取环境初始化完成"
log_info "  Chrome 路径: ${CHROME_PATH:-未配置}"
log_info "  Truststore: ${TRUSTSTORE}"
log_info "  环境变量已写入: ${ENV_FILE}"
