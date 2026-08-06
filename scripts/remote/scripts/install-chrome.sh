#!/bin/bash
# ===================================================================
# 爱创作 - 服务器 Chrome/Chromium 一键安装脚本
# 运行环境：Linux 服务器（root 或具有 sudo 权限）
# 用途：为热搜 CDP 抓取安装 headless Chrome/Chromium
# 特性：
#   - 自动识别 Ubuntu/Debian/CentOS/RHEL/Rocky/Alma/Amazon Linux
#   - 优先安装 Chromium（依赖少、无 Google 仓库也可装），否则回退到 Google Chrome
#   - 幂等，重复执行不会重复安装
# 用法：
#   chmod +x install-chrome.sh
#   ./install-chrome.sh
# 安装完成后可运行 setup-hotsearch-env.sh 将路径写入 .env
# ===================================================================

set -e

log_info() { echo -e "\033[0;32m[INFO]\033[0m $1"; }
log_warn() { echo -e "\033[1;33m[WARN]\033[0m $1"; }
log_error() { echo -e "\033[0;31m[ERROR]\033[0m $1"; }

 detect_distro() {
    if [ -f /etc/os-release ]; then
        # shellcheck source=/dev/null
        . /etc/os-release
        echo "$ID"
    else
        echo "unknown"
    fi
}

PKG_MANAGER=""
DISTRO=$(detect_distro)

case "$DISTRO" in
    ubuntu|debian)
        PKG_MANAGER="apt-get"
        ;;
    centos|rhel|rocky|almalinux|amzn|fedora)
        if command -v dnf >/dev/null 2>&1; then
            PKG_MANAGER="dnf"
        else
            PKG_MANAGER="yum"
        fi
        ;;
    alpine)
        PKG_MANAGER="apk"
        ;;
    *)
        log_error "不支持的 Linux 发行版: ${DISTRO}"
        log_error "请手动安装 Google Chrome 或 Chromium 后配置 HOT_SEARCH_CHROME_PATH"
        exit 1
        ;;
esac

find_chrome() {
    local candidate
    for candidate in \
        /usr/bin/google-chrome \
        /usr/bin/google-chrome-stable \
        /usr/bin/microsoft-edge \
        /usr/bin/microsoft-edge-stable \
        /usr/bin/chromium \
        /usr/bin/chromium-browser \
        /usr/local/bin/google-chrome \
        /usr/local/bin/chromium \
        /usr/local/bin/chromium-browser \
        /opt/google/chrome/google-chrome
    do
        if [ -x "$candidate" ]; then
            echo "$candidate"
            return 0
        fi
    done
    return 1
}

install_chromium_apt() {
    log_info "正在更新 apt 索引..."
    apt-get update -qq

    # 不同 Debian/Ubuntu 版本 chromium 包名不同，先尝试最常见的几个
    local pkg
    for pkg in chromium chromium-browser; do
        if apt-cache show "$pkg" >/dev/null 2>&1; then
            log_info "安装 ${pkg}..."
            DEBIAN_FRONTEND=noninteractive apt-get install -y -qq "$pkg"
            return 0
        fi
    done

    log_warn "apt 源中未找到 Chromium 包，尝试安装 Google Chrome..."
    install_google_chrome_apt
}

install_google_chrome_apt() {
    log_info "添加 Google Chrome APT 源..."
    apt-get update -qq
    apt-get install -y -qq wget gnupg

    local keyring="/usr/share/keyrings/google-linux-signing-keyring.gpg"
    wget -qO- https://dl.google.com/linux/linux_signing_key.pub | gpg --dearmor > "$keyring"

    echo "deb [arch=amd64 signed-by=${keyring}] http://dl.google.com/linux/chrome/deb/ stable main" \
        > /etc/apt/sources.list.d/google-chrome.list

    apt-get update -qq
    DEBIAN_FRONTEND=noninteractive apt-get install -y -qq google-chrome-stable
}

install_microsoft_edge_apt() {
    log_info "添加 Microsoft Edge APT 源..."
    apt-get update -qq
    apt-get install -y -qq wget gnupg

    local keyring="/usr/share/keyrings/microsoft-edge.gpg"
    wget -qO- https://packages.microsoft.com/keys/microsoft.asc | gpg --dearmor > "$keyring"

    echo "deb [arch=amd64 signed-by=${keyring}] https://packages.microsoft.com/repos/edge stable main" \
        > /etc/apt/sources.list.d/microsoft-edge.list

    apt-get update -qq
    DEBIAN_FRONTEND=noninteractive apt-get install -y -qq microsoft-edge-stable
}

install_google_chrome_yum() {
    log_info "添加 Google Chrome YUM 源..."
    cat > /etc/yum.repos.d/google-chrome.repo <<'EOF'
[google-chrome]
name=google-chrome
baseurl=https://dl.google.com/linux/chrome/rpm/stable/x86_64
enabled=1
gpgcheck=1
gpgkey=https://dl-ssl.google.com/linux/linux_signing_key.pub
EOF

    log_info "安装 Google Chrome..."
    $PKG_MANAGER install -y google-chrome-stable
}

install_microsoft_edge_yum() {
    log_info "添加 Microsoft Edge YUM 源..."
    cat > /etc/yum.repos.d/microsoft-edge.repo <<'EOF'
[microsoft-edge]
name=microsoft-edge
baseurl=https://packages.microsoft.com/yumrepos/edge/
enabled=1
gpgcheck=1
gpgkey=https://packages.microsoft.com/keys/microsoft.asc
EOF

    log_info "安装 Microsoft Edge..."
    $PKG_MANAGER install -y microsoft-edge-stable
}

install_chromium_yum_fallback() {
    log_info "尝试安装 Chromium（可能需要 EPEL 源）..."
    $PKG_MANAGER install -y chromium
}

install_apk() {
    log_info "安装 Chromium..."
    apk add --no-cache chromium
}

install_chrome_deps_yum() {
    log_info "安装 headless Chrome 运行依赖..."
    $PKG_MANAGER install -y \
        atk at-spi2-atk cups-libs libXcomposite libXcursor libXdamage libXext \
        libXi libXrandr libXScrnSaver libXtst pango xorg-x11-fonts-Type1 \
        xorg-x11-fonts-misc alsa-lib nss mesa-libgbm liberation-fonts vulkan
}

install_chrome_deps_apt() {
    log_info "安装 headless Chrome 运行依赖..."
    apt-get update -qq
    DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
        libatk1.0-0 libatk-bridge2.0-0 libcups2 libxcomposite1 libxrandr2 \
        libxdamage1 libxext6 libxi6 libxss1 libxtst6 libnss3 libgbm1 \
        libasound2 fonts-liberation xdg-utils
}

# 主流程
log_info "检测到包管理器: ${PKG_MANAGER}, 发行版: ${DISTRO}"

if CHROME_PATH=$(find_chrome); then
    # yum/dnf 上若只装了 Chromium，额外尝试装 Google Chrome / Microsoft Edge（避免 EPEL Chromium 与系统 gnutls 等库冲突）
    if [[ "$CHROME_PATH" == *"chromium"* ]] && { [[ "$PKG_MANAGER" == "yum" ]] || [[ "$PKG_MANAGER" == "dnf" ]]; }; then
        log_warn "检测到已安装 Chromium，尝试额外安装 Google Chrome / Microsoft Edge 以避免系统库冲突..."
        if install_google_chrome_yum; then
            CHROME_PATH=$(find_chrome)
        elif install_microsoft_edge_yum; then
            CHROME_PATH=$(find_chrome)
        else
            log_warn "Google Chrome / Microsoft Edge 均安装失败，继续使用现有 Chromium"
        fi
    else
        log_info "Chrome/Chromium 已存在: ${CHROME_PATH}，跳过安装"
    fi
else
    log_info "未找到 Chrome/Chromium，开始安装..."
    case "$PKG_MANAGER" in
        apt-get)
            if install_google_chrome_apt; then
                : # success
            elif install_microsoft_edge_apt; then
                : # success
            else
                log_warn "Google Chrome / Microsoft Edge 均安装失败，回退到 Chromium..."
                install_chromium_apt
            fi
            ;;
        yum|dnf)
            if install_google_chrome_yum; then
                : # success
            elif install_microsoft_edge_yum; then
                : # success
            else
                log_warn "Google Chrome / Microsoft Edge 均安装失败，回退到 Chromium..."
                install_chromium_yum_fallback
            fi
            ;;
        apk)
            install_apk
            ;;
    esac
fi

if CHROME_PATH=$(find_chrome); then
    log_info "安装完成，Chrome/Chromium 路径: ${CHROME_PATH}"

    case "$PKG_MANAGER" in
        apt-get)
            install_chrome_deps_apt || log_warn "依赖安装失败，Chrome 可能无法以 headless 模式启动"
            ;;
        yum|dnf)
            install_chrome_deps_yum || log_warn "依赖安装失败，Chrome 可能无法以 headless 模式启动"
            ;;
    esac

    log_info "建议执行 ./setup-hotsearch-env.sh 将路径写入 /root/app/aichuangzuo/.env"
else
    log_error "安装后仍未找到 Chrome/Chromium，请检查包管理器输出或手动安装"
    exit 1
fi
