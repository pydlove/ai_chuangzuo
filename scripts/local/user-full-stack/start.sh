#!/bin/bash
# 启动用户端后端 + 前端（一体化开发环境）
# 用法：./start.sh
#
# 环境变量（可选）：
#   MYSQL_USERNAME / MYSQL_PASSWORD  数据库账号，默认 root / 123456
#   AUTH_CAPTCHA_MOCK_ENABLED        true|false，开发时设为 true 可跳过图形验证码
#   AUTH_CAPTCHA_MOCK_CODE           配合上面的验证码固定值
#   AUTH_EMAIL_CODE_MOCK_ENABLED     true|false，开发时设为 true 可跳过邮箱验证
#   AUTH_EMAIL_CODE_MOCK_CODE        配合上面的邮箱验证码固定值

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

# 默认账号：可通过环境变量覆盖
export MYSQL_USERNAME="${MYSQL_USERNAME:-root}"
export MYSQL_PASSWORD="${MYSQL_PASSWORD:-123456}"

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
start_frontend

echo ""
echo "等待服务启动..."
wait_for_port "${BACKEND_PORT}" "后端" "${BACKEND_LOG}"
wait_for_port "${FRONTEND_PORT}" "前端" "${FRONTEND_LOG}"

echo ""
echo "========================================"
echo "  用户端全栈启动完成"
echo "========================================"
echo "  前端地址：http://localhost:${FRONTEND_PORT}"
echo "  后端地址：http://localhost:${BACKEND_PORT}"
echo "  API 文档：http://localhost:${BACKEND_PORT}/doc.html"
echo "  日志目录：${LOG_DIR}"
echo ""
echo "查看日志：tail -f ${BACKEND_LOG}"
echo "停止服务：./stop.sh"