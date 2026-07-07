#!/bin/bash
# 启动用户端后端 + 前端（一体化开发环境）
# 用法：./start.sh
#
# 环境变量（可选）：
#   MYSQL_USERNAME / MYSQL_PASSWORD  数据库账号，默认 root / 123456
#   SPRING_MAIL_HOST / PORT / USERNAME / PASSWORD   真实 SMTP 配置（默认 126）
#   JASYPT_ENCRYPTOR_PASSWORD        解密 ENC(...) 密码的主密钥
#
# 如需自定义敏感配置，可在同级目录建 .env 文件（已被 git 忽略），格式：
#   SPRING_MAIL_PASSWORD=ENC(...)
#   JASYPT_ENCRYPTOR_PASSWORD=...

set -e

ROOT_DIR="$(cd "$(dirname "$0")/../../.." && pwd)"
LOG_DIR="${ROOT_DIR}/logs"
PID_DIR="${ROOT_DIR}/logs/.pids"
BACKEND_PORT=25050
FRONTEND_PORT=22345
BACKEND_PID_FILE="${PID_DIR}/user-api.pid"
FRONTEND_PID_FILE="${PID_DIR}/user-web.pid"
BACKEND_LOG="${LOG_DIR}/user-api.log"
FRONTEND_LOG="${LOG_DIR}/user-web.log"

# 加载本地 .env（可选，便于覆盖敏感配置）
if [ -f "${ROOT_DIR}/scripts/local/user-full-stack/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  . "${ROOT_DIR}/scripts/local/user-full-stack/.env"
  set +a
fi

# 数据库账号（默认开发库 root / 123456）
export MYSQL_USERNAME="${MYSQL_USERNAME:-root}"
export MYSQL_PASSWORD="${MYSQL_PASSWORD:-123456}"

# 126 邮箱 SMTP：本地默认走真实 SMTP，让本机真能发验证码
export SPRING_MAIL_HOST="${SPRING_MAIL_HOST:-smtp.126.com}"
export SPRING_MAIL_PORT="${SPRING_MAIL_PORT:-465}"
export SPRING_MAIL_USERNAME="${SPRING_MAIL_USERNAME:-aiocloud@126.com}"
export SPRING_MAIL_PASSWORD="${SPRING_MAIL_PASSWORD:-ENC(hHb3OUbIanYmYyMYLSBz1WBtSIgAGZCSeNrIw5SG7BAnQnx6z7/bWx5UGyfI7XRl47T/HC7i3DQjamBSRIOujQ==)}"

# Jasypt 主密钥（与 application-*.yml 中 ENC(...) 配对）
export JASYPT_ENCRYPTOR_PASSWORD="${JASYPT_ENCRYPTOR_PASSWORD:-MySecretKey2024}"

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
  echo "  启动用户端后端（Spring Boot, 端口 ${BACKEND_PORT}）"
  echo "========================================"
  cd "${ROOT_DIR}/project" || return 1
  # 先把 shared 模块装到本地仓库，避免 user/api 找不到依赖
  mvn -pl shared install -DskipTests -q
  nohup mvn -pl user/api spring-boot:run -DskipTests \
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
  echo "  启动用户端前端（Vite, 端口 ${FRONTEND_PORT}）"
  echo "========================================"
  cd "${ROOT_DIR}/project/user/web" || return 1
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

echo ""
echo "等待服务启动..."
wait_for_port "${BACKEND_PORT}" "后端" "${BACKEND_LOG}"

start_frontend
wait_for_port "${FRONTEND_PORT}" "前端" "${FRONTEND_LOG}"

echo ""
echo "========================================"
echo "  用户端全栈启动完成"
echo "========================================"
echo "  本机访问：http://localhost:${FRONTEND_PORT}"
echo "  后端地址：http://localhost:${BACKEND_PORT}"
echo "  API 文档：http://localhost:${BACKEND_PORT}/doc.html"
LAN_IP=$(get_lan_ip)
if [ -n "$LAN_IP" ]; then
  echo ""
  echo "  局域网访问（手机/其他设备）："
  echo "    http://${LAN_IP}:${FRONTEND_PORT}"
fi
echo ""
echo "  日志目录：${LOG_DIR}"
echo ""
echo "查看日志：tail -f ${BACKEND_LOG}"
echo "停止服务：./stop.sh"