#!/bin/bash
# 启动管理端后端 + 前端（一体化开发环境）
# 用法：./start.sh
#
# 环境变量（可选）：
#   MYSQL_USERNAME / MYSQL_PASSWORD  数据库账号，默认 root / 123456
#   ADMIN_JWT_SECRET                 管理端 JWT Secret，默认仅开发使用
#
# 如需自定义敏感配置，可在同级目录建 .env 文件（已被 git 忽略），格式：
#   ADMIN_JWT_SECRET=...

set -e

ROOT_DIR="$(cd "$(dirname "$0")/../../.." && pwd)"
LOG_DIR="${ROOT_DIR}/logs"
PID_DIR="${ROOT_DIR}/logs/.pids"
BACKEND_PORT=26060
FRONTEND_PORT=22346
BACKEND_PID_FILE="${PID_DIR}/admin-api.pid"
FRONTEND_PID_FILE="${PID_DIR}/admin-web.pid"
BACKEND_LOG="${LOG_DIR}/admin-api.log"
FRONTEND_LOG="${LOG_DIR}/admin-web.log"

# 加载本地 .env（可选，便于覆盖敏感配置）
if [ -f "${ROOT_DIR}/scripts/local/admin-full-stack/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  . "${ROOT_DIR}/scripts/local/admin-full-stack/.env"
  set +a
fi

# 数据库账号（默认开发库 root / 123456）
export MYSQL_USERNAME="${MYSQL_USERNAME:-root}"
export MYSQL_PASSWORD="${MYSQL_PASSWORD:-123456}"

# 管理端 JWT Secret（开发默认值，生产必须通过环境变量注入）
export ADMIN_JWT_SECRET="${ADMIN_JWT_SECRET:-dev-admin-jwt-secret-must-be-replaced-in-production-2026}"

# 获取本机局域网 IP（macOS/Linux 兼容）
get_lan_ip() {
  local ip=""
  if command -v ip >/dev/null 2>&1; then
    ip=$(ip route get 1.1.1.1 2>/dev/null | awk '{for(i=1;i<=NF;i++) if($i=="src") {print $(i+1); exit}}')
  fi
  if [ -z "$ip" ] && command -v ifconfig >/dev/null 2>&1; then
    ip=$(ifconfig | grep "inet " | grep -v "127.0.0.1" | head -1 | awk '{print $2}')
  fi
  if [ -z "$ip" ] && command -v hostname >/dev/null 2>&1; then
    ip=$(hostname -I 2>/dev/null | awk '{print $1}')
  fi
  echo "$ip"
}

mkdir -p "${LOG_DIR}" "${PID_DIR}"

# 启动后端
start_backend() {
  if [ -f "${BACKEND_PID_FILE}" ] && kill -0 "$(cat "${BACKEND_PID_FILE}")" 2>/dev/null; then
    echo "后端已在运行（PID $(cat "${BACKEND_PID_FILE}"))"
    return 0
  fi

  echo "========================================"
  echo "  启动管理端后端（Spring Boot, 端口 ${BACKEND_PORT}）"
  echo "========================================"
  cd "${ROOT_DIR}/project" || return 1
  # 先把 shared 模块装到本地仓库，避免 admin/api 找不到依赖
  mvn -pl shared install -DskipTests -q

  # dev 环境：注入 DLP CA truststore（baidu/weibo 自签 CA，沙箱 JDK 17 cacerts 未含）
  # 生产用 Linux 系统 cacerts 默认信任，无需 truststore
  local truststore="${ROOT_DIR}/config/ssl/admin-api-truststore.p12"
  local extra_jvm_opts=""
  if [ -f "${truststore}" ]; then
    extra_jvm_opts="-Djavax.net.ssl.trustStore=${truststore} -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStoreType=PKCS12"
    echo "已注入 dev truststore：${truststore}"
  fi

  nohup env _JAVA_OPTIONS="${extra_jvm_opts}" \
    mvn -pl admin/api spring-boot:run -Dspring-boot.run.jvmArguments="${extra_jvm_opts}" -DskipTests \
    > "${BACKEND_LOG}" 2>&1 &
  echo $! > "${BACKEND_PID_FILE}"
  echo "后端 PID：$(cat "${BACKEND_PID_FILE}")，日志：${BACKEND_LOG}"
}

# 启动前端
start_frontend() {
  if [ -f "${FRONTEND_PID_FILE}" ] && kill -0 "$(cat "${FRONTEND_PID_FILE}")" 2>/dev/null; then
    echo "前端已在运行（PID $(cat "${FRONTEND_PID_FILE}")）"
    return 0
  fi

  echo "========================================"
  echo "  启动管理端前端（Vite, 端口 ${FRONTEND_PORT}）"
  echo "========================================"
  cd "${ROOT_DIR}/project/admin/web" || return 1
  nohup npm run dev -- --host > "${FRONTEND_LOG}" 2>&1 &
  echo $! > "${FRONTEND_PID_FILE}"
  echo "前端 PID：$(cat "${FRONTEND_PID_FILE}")，日志：${FRONTEND_LOG}"
}

# 等到端口可访问（最多 90 秒）
wait_for_port() {
  local port=$1
  local name=$2
  local max=90
  for ((i=1; i<=max; i++)); do
    if lsof -ti:"${port}" >/dev/null 2>&1; then
      echo "${name}已就绪（端口 ${port}）"
      return 0
    fi
    sleep 1
  done
  echo "警告：${name}在 ${max} 秒内未监听端口 ${port}，请查看日志 ${3:-}"
  return 1
}

start_backend
start_frontend

echo ""
echo "等待服务启动..."
wait_for_port "${BACKEND_PORT}" "后端" "${BACKEND_LOG}"
wait_for_port "${FRONTEND_PORT}" "前端" "${FRONTEND_LOG}"

echo ""
echo "========================================"
echo "  管理端全栈启动完成"
echo "========================================"
echo "  登录页面：http://localhost:${FRONTEND_PORT}/login"
echo "  后端地址：http://localhost:${BACKEND_PORT}"
echo "  API 文档：http://localhost:${BACKEND_PORT}/admin/doc.html"
LAN_IP=$(get_lan_ip)
if [ -n "$LAN_IP" ]; then
  echo ""
  echo "  局域网访问（手机/其他设备）："
  echo "    http://${LAN_IP}:${FRONTEND_PORT}/login"
fi
echo ""
echo "  日志目录：${LOG_DIR}"
echo ""
echo "查看日志：tail -f ${BACKEND_LOG}"
echo "停止服务：./stop.sh"
