#!/bin/bash
# 停止管理控制台前端开发服务器
# 用法：./stop.sh

PORT=22346
PIDS=$(lsof -ti:"${PORT}" 2>/dev/null)

if [ -n "${PIDS}" ]; then
  echo "${PIDS}" | xargs kill -9 2>/dev/null
  echo "管理控制台前端服务（端口 ${PORT}）已停止"
else
  echo "端口 ${PORT} 无运行中的进程"
fi
