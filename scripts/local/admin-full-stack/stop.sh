#!/bin/bash
# 停止管理端后端 + 前端
# 用法：./stop.sh

ROOT_DIR="$(cd "$(dirname "$0")/../../.." && pwd)"
PID_DIR="${ROOT_DIR}/logs/.pids"
BACKEND_PID_FILE="${PID_DIR}/admin-api.pid"
FRONTEND_PID_FILE="${PID_DIR}/admin-web.pid"
BACKEND_PORT=26060
FRONTEND_PORT=22346

stop_one() {
  local name=$1
  local pid_file=$2
  local port=$3

  if [ -f "${pid_file}" ]; then
    local pid
    pid=$(cat "${pid_file}")
    if kill -0 "${pid}" 2>/dev/null; then
      echo "停止${name}（PID ${pid}）..."
      kill "${pid}" 2>/dev/null || true
      # 等待 5 秒，未结束则强杀
      for ((i=1; i<=5; i++)); do
        if ! kill -0 "${pid}" 2>/dev/null; then break; fi
        sleep 1
      done
      if kill -0 "${pid}" 2>/dev/null; then
        kill -9 "${pid}" 2>/dev/null || true
      fi
    fi
    rm -f "${pid_file}"
  fi

  # 兜底：清理端口残留进程（Spring Boot / Vite 子进程）
  local leftovers
  leftovers=$(lsof -ti:"${port}" 2>/dev/null || true)
  if [ -n "${leftovers}" ]; then
    echo "清理${name}端口 ${port} 残留进程：${leftovers}"
    echo "${leftovers}" | xargs kill -9 2>/dev/null || true
  fi
}

stop_one "后端" "${BACKEND_PID_FILE}" "${BACKEND_PORT}"
stop_one "前端" "${FRONTEND_PID_FILE}" "${FRONTEND_PORT}"

echo "管理端全栈已停止"
