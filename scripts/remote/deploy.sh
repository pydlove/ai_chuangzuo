#!/bin/bash
set -e

# ===================================================================
# 爱创作 (AI Creation) - 一键远程部署脚本
# 运行环境：macOS / Linux（本地机器）
# 目标服务器：Linux（CentOS/Ubuntu 等）
# ===================================================================

# ============ 配置区（请按实际情况修改） ============
SERVER_IP="101.126.15.58"           # 例如: 123.45.67.89
SERVER_USER="root"                 # SSH 用户名
SERVER_PASSWORD="Pydlove520smy@1"   # SSH 密码（或使用 SSH_KEY_PATH）
SSH_KEY_PATH="~/.ssh/id_rsa"                    # 如使用密钥登录，填密钥路径，例如: ~/.ssh/id_rsa
SERVER_DOMAIN="www.mmshuo.tech" # 你的域名（用于 nginx server_name）
NGINX_SSL_CERT="/root/ssl/www.mmshuo.tech.pem"      # SSL 证书路径
NGINX_SSL_KEY="/root/ssl/www.mmshuo.tech.key"          # SSL 私钥路径

# 业务端口
USER_FRONTEND_PORT=22345
USER_BACKEND_PORT=22346
ADMIN_FRONTEND_PORT=22347
ADMIN_BACKEND_PORT=22348
# ===================================================

# 项目根目录（脚本位于 scripts/remote/ 下）
DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$DEPLOY_DIR/../.." && pwd)"
BACKEND_PROJECT_DIR="$PROJECT_DIR/project"

# 远程部署目录
REMOTE_APP_DIR="/root/app/aichuangzuo"
REMOTE_NGINX_CONF="/etc/nginx/nginx.conf"
NGINX_CONF_SOURCE="$(dirname "$PROJECT_DIR")/nginx/nginx.conf"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

function log_info() {
  echo -e "${GREEN}[INFO]${NC} $1"
}

function log_warn() {
  echo -e "${YELLOW}[WARN]${NC} $1"
}

function log_error() {
  echo -e "${RED}[ERROR]${NC} $1"
}

function log_step() {
  echo -e "${BLUE}[STEP]${NC} $1"
}

function retry_scp() {
    local src=$1; local dst=$2
    local max_attempts=3; local delay=10
    local scp_opts=""
    [ -d "$src" ] && scp_opts="-r"
    for attempt in $(seq 1 $max_attempts); do
        if eval "$SCP_CMD $scp_opts $src $dst"; then
            return 0
        fi
        log_warn "SCP attempt $attempt/$max_attempts failed, retrying in ${delay}s..."
        sleep $delay
        delay=$((delay * 2))
    done
    log_error "SCP failed after $max_attempts attempts"
    return 1
}

# SSH 连接复用（避免短时间内大量连接被服务器 reset）
SSH_MUX_PATH="/tmp/ssh_mux_${SERVER_IP}_${SERVER_USER}"
SSH_MUX_OPTS="-o ControlMaster=auto -o ControlPath=$SSH_MUX_PATH -o ControlPersist=600"

# SSH 连接参数
if [ -n "$SSH_KEY_PATH" ] && [ -f "$SSH_KEY_PATH" ]; then
  SSH_CMD="ssh -i $SSH_KEY_PATH -o StrictHostKeyChecking=no $SSH_MUX_OPTS"
  SCP_CMD="scp -i $SSH_KEY_PATH -o StrictHostKeyChecking=no $SSH_MUX_OPTS"
else
  if ! command -v sshpass &> /dev/null; then
    log_error "未安装 sshpass，请执行: brew install sshpass (macOS) 或 apt-get install sshpass (Linux)"
    log_error "或者配置 SSH_KEY_PATH 使用密钥登录"
    exit 1
  fi
  SSH_CMD="sshpass -p '$SERVER_PASSWORD' ssh -o StrictHostKeyChecking=no -o ConnectTimeout=30 -o ServerAliveInterval=30 -o ServerAliveCountMax=3 $SSH_MUX_OPTS"
  SCP_CMD="sshpass -p '$SERVER_PASSWORD' scp -o StrictHostKeyChecking=no -o ConnectTimeout=30 -o ServerAliveInterval=30 -o ServerAliveCountMax=3 $SSH_MUX_OPTS"
fi

REMOTE_HOST="$SERVER_USER@$SERVER_IP"

# 参数校验
if [ -z "$SERVER_IP" ]; then
  log_error "SERVER_IP 未配置，请修改脚本顶部的配置区"
  exit 1
fi

# 检查本地环境变量文件
if [ ! -f "$DEPLOY_DIR/.env" ]; then
  log_warn "未找到 $DEPLOY_DIR/.env 文件，将创建一个空文件上传"
  touch "$DEPLOY_DIR/.env"
fi

# ============ 工具函数 ============

function has_pom() {
    [ -f "$1/pom.xml" ]
}

function build_frontend() {
    local name=$1
    local dir=$2
    cd "$dir"
    if [ ! -d "node_modules" ]; then
        log_info "安装 ${name} 依赖..."
        npm install
    fi
    log_info "构建 ${name} ..."
    npm run build
}

function build_backend() {
    local name=$1
    local dir=$2
    cd "$BACKEND_PROJECT_DIR"
    log_info "构建 ${name} ..."
    mvn clean package -pl "$dir" -am -DskipTests
}

# ============ 步骤1: 本地构建 ============
log_step "开始本地构建..."

# 用户端前端
build_frontend "用户端前端" "$PROJECT_DIR/project/user/web"

# 管理端前端
if [ -d "$PROJECT_DIR/project/admin/web" ]; then
    build_frontend "管理端前端" "$PROJECT_DIR/project/admin/web"
else
    log_warn "管理端前端目录不存在，跳过"
fi

# 用户端后端
if has_pom "$PROJECT_DIR/project/user/api"; then
    build_backend "用户端后端" "user/api"
else
    log_error "用户端后端 pom.xml 不存在，无法构建"
    exit 1
fi

# 管理端后端
if has_pom "$PROJECT_DIR/project/admin/api"; then
    build_backend "管理端后端" "admin/api"
else
    log_warn "管理端后端 pom.xml 不存在，跳过构建"
fi

log_info "本地构建完成"

# ============ 步骤2: 在服务器创建目录结构 ============
log_step "准备服务器目录..."
eval "$SSH_CMD $REMOTE_HOST 'mkdir -p \
  $REMOTE_APP_DIR/user-web \
  $REMOTE_APP_DIR/admin-web \
  $REMOTE_APP_DIR/user-api \
  $REMOTE_APP_DIR/admin-api \
  $REMOTE_APP_DIR/scripts \
  $REMOTE_APP_DIR/systemd \
  $REMOTE_APP_DIR/logs'"

# ============ 步骤3: 上传前端静态文件 ============
log_step "上传用户端前端到 $REMOTE_APP_DIR/user-web/ ..."
eval "$SSH_CMD $REMOTE_HOST 'rm -rf $REMOTE_APP_DIR/user-web/*'"
retry_scp "$PROJECT_DIR/project/user/web/dist/." "$REMOTE_HOST:$REMOTE_APP_DIR/user-web/"

if [ -d "$PROJECT_DIR/project/admin/web/dist" ]; then
    log_step "上传管理端前端到 $REMOTE_APP_DIR/admin-web/ ..."
    eval "$SSH_CMD $REMOTE_HOST 'rm -rf $REMOTE_APP_DIR/admin-web/*'"
    retry_scp "$PROJECT_DIR/project/admin/web/dist/." "$REMOTE_HOST:$REMOTE_APP_DIR/admin-web/"
fi

# ============ 步骤4: 上传后端 JAR 包和配置 ============
USER_JAR="$PROJECT_DIR/project/user/api/target/user-api-1.0.0-SNAPSHOT.jar"
ADMIN_JAR="$PROJECT_DIR/project/admin/api/target/admin-api-1.0.0-SNAPSHOT.jar"

if [ ! -f "$USER_JAR" ]; then
    log_error "用户端后端 JAR 不存在: $USER_JAR"
    exit 1
fi

log_step "上传用户端后端 JAR ..."
retry_scp "$USER_JAR" "$REMOTE_HOST:$REMOTE_APP_DIR/user-api/user-api.jar"

# 上传 application-prod.yml（如果存在）
if [ -f "$PROJECT_DIR/project/user/api/src/main/resources/application-prod.yml" ]; then
    retry_scp "$PROJECT_DIR/project/user/api/src/main/resources/application-prod.yml" "$REMOTE_HOST:$REMOTE_APP_DIR/user-api/"
fi

if has_pom "$PROJECT_DIR/project/admin/api"; then
    if [ ! -f "$ADMIN_JAR" ]; then
        log_error "管理端后端 JAR 不存在: $ADMIN_JAR"
        exit 1
    fi
    log_step "上传管理端后端 JAR ..."
    retry_scp "$ADMIN_JAR" "$REMOTE_HOST:$REMOTE_APP_DIR/admin-api/admin-api.jar"
    if [ -f "$PROJECT_DIR/project/admin/api/src/main/resources/application-prod.yml" ]; then
        retry_scp "$PROJECT_DIR/project/admin/api/src/main/resources/application-prod.yml" "$REMOTE_HOST:$REMOTE_APP_DIR/admin-api/"
    fi
fi

# ============ 步骤5: 上传环境变量、systemd service 与启停脚本 ============
log_step "上传环境变量文件..."
retry_scp "$DEPLOY_DIR/.env" "$REMOTE_HOST:$REMOTE_APP_DIR/.env"
eval "$SSH_CMD $REMOTE_HOST 'chmod 600 $REMOTE_APP_DIR/.env'"

log_step "上传 systemd service 文件..."
retry_scp "$DEPLOY_DIR/systemd/aichuangzuo-user-api.service" "$REMOTE_HOST:/etc/systemd/system/"
if has_pom "$PROJECT_DIR/project/admin/api"; then
    retry_scp "$DEPLOY_DIR/systemd/aichuangzuo-admin-api.service" "$REMOTE_HOST:/etc/systemd/system/"
fi
eval "$SSH_CMD $REMOTE_HOST 'systemctl daemon-reload && systemctl enable aichuangzuo-user-api'"
if has_pom "$PROJECT_DIR/project/admin/api"; then
    eval "$SSH_CMD $REMOTE_HOST 'systemctl enable aichuangzuo-admin-api'"
fi

log_step "上传启停脚本..."
retry_scp "$DEPLOY_DIR/scripts/start-all.sh" "$REMOTE_HOST:$REMOTE_APP_DIR/scripts/"
retry_scp "$DEPLOY_DIR/scripts/stop-all.sh" "$REMOTE_HOST:$REMOTE_APP_DIR/scripts/"
retry_scp "$DEPLOY_DIR/scripts/restart-all.sh" "$REMOTE_HOST:$REMOTE_APP_DIR/scripts/"
retry_scp "$DEPLOY_DIR/scripts/status.sh" "$REMOTE_HOST:$REMOTE_APP_DIR/scripts/"
eval "$SSH_CMD $REMOTE_HOST 'chmod +x $REMOTE_APP_DIR/scripts/*.sh && \
  cp $REMOTE_APP_DIR/scripts/start-all.sh $REMOTE_APP_DIR/start-all.sh && \
  cp $REMOTE_APP_DIR/scripts/stop-all.sh $REMOTE_APP_DIR/stop-all.sh && \
  cp $REMOTE_APP_DIR/scripts/restart-all.sh $REMOTE_APP_DIR/restart-all.sh && \
  cp $REMOTE_APP_DIR/scripts/status.sh $REMOTE_APP_DIR/status.sh && \
  chmod +x $REMOTE_APP_DIR/*.sh'"

# ============ 步骤6: 上传 Nginx 配置 ============
log_step "上传 Nginx 配置..."

TMP_NGINX="/tmp/aichuangzuo_nginx.conf"

if [ ! -f "$NGINX_CONF_SOURCE" ]; then
  log_error "未找到本地 Nginx 配置文件: $NGINX_CONF_SOURCE"
  exit 1
fi

# 复制到临时文件，按需替换域名
cp "$NGINX_CONF_SOURCE" "$TMP_NGINX"

# 如需绑定域名，可在此替换（默认 server_name localhost，保留即可）
# if [ -n "$SERVER_DOMAIN" ] && [ "$SERVER_DOMAIN" != "localhost" ]; then
#   sed -i.bak -e "s/server_name localhost/server_name $SERVER_DOMAIN/g" "$TMP_NGINX"
# fi

retry_scp "$TMP_NGINX" "$REMOTE_HOST:$REMOTE_NGINX_CONF"
eval "$SSH_CMD $REMOTE_HOST 'nginx -t'"
rm -f "$TMP_NGINX"

# ============ 步骤7: 重启后端服务 ============
log_step "重启后端服务..."
eval "$SSH_CMD $REMOTE_HOST 'systemctl stop aichuangzuo-user-api 2>/dev/null || true'"
if has_pom "$PROJECT_DIR/project/admin/api"; then
    eval "$SSH_CMD $REMOTE_HOST 'systemctl stop aichuangzuo-admin-api 2>/dev/null || true'"
fi

sleep 2

# 清理残留端口
eval "$SSH_CMD $REMOTE_HOST 'lsof -t -i:$USER_BACKEND_PORT >/dev/null 2>&1 && { echo "清理用户端后端残留进程..."; kill -9 \$(lsof -t -i:$USER_BACKEND_PORT) 2>/dev/null; } || true'"
eval "$SSH_CMD $REMOTE_HOST 'lsof -t -i:$ADMIN_BACKEND_PORT >/dev/null 2>&1 && { echo "清理管理端后端残留进程..."; kill -9 \$(lsof -t -i:$ADMIN_BACKEND_PORT) 2>/dev/null; } || true'"

sleep 1

log_info "启动用户端后端（systemctl）..."
eval "$SSH_CMD $REMOTE_HOST 'systemctl start aichuangzuo-user-api'"

if has_pom "$PROJECT_DIR/project/admin/api"; then
    log_info "启动管理端后端（systemctl）..."
    eval "$SSH_CMD $REMOTE_HOST 'systemctl start aichuangzuo-admin-api'"
fi

log_step "重载 Nginx..."
eval "$SSH_CMD $REMOTE_HOST 'nginx -s reload'"

# 关闭 SSH 连接复用的 master 连接
log_step "清理 SSH 连接复用..."
eval "$SSH_CMD -O exit $REMOTE_HOST" 2>/dev/null || true
rm -f "$SSH_MUX_PATH"

# ============ 部署完成 ============
log_info "========================================"
log_info "部署完成！"
log_info "========================================"
log_info "用户端访问: http://$SERVER_IP:$USER_FRONTEND_PORT"
log_info "管理端访问: http://$SERVER_IP:$ADMIN_FRONTEND_PORT"
log_info "用户端 API: http://$SERVER_IP:$USER_FRONTEND_PORT/api"
log_info "管理端 API: http://$SERVER_IP:$ADMIN_FRONTEND_PORT/api"
log_info "========================================"
log_warn "重要提醒:"
log_warn "1. 首次部署前请确保服务器已安装 Java 17、Nginx、MySQL 8"
log_warn "2. 请编辑 $DEPLOY_DIR/.env 写入 JASYPT_ENCRYPTOR_PASSWORD 等敏感配置"
log_warn "3. 如需 HTTPS，请配置 NGINX_SSL_CERT 和 NGINX_SSL_KEY，并修改 nginx 配置"
log_warn "4. 数据库迁移 Flyway 会在应用启动时自动执行"
log_info "========================================"
