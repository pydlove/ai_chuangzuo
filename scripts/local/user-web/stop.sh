#!/bin/bash
# 停止用户端前端开发服务器
# 用法：./stop.sh

PORT=22345
PIDS=$(lsof -ti:"${PORT}" 2>/dev/null)

if [ -n "${PIDS}" ]; then
  echo "${PIDS}" | xargs kill -9 2>/dev/null
  echo "用户端前端服务（端口 ${PORT}）已停止"
else
  echo "端口 ${PORT} 无运行中的进程"
fi
