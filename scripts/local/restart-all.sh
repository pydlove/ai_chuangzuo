#!/bin/bash
# 一键重启 用户端 + 管理端 全栈（后端 + 前端）
# 用法：./restart-all.sh
#
# 串行执行，避免两边同时跑 mvn -pl shared install 造成本地仓库竞争。

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "========================================"
echo "  [1/2] 重启用户端全栈"
echo "========================================"
sh "${SCRIPT_DIR}/user-full-stack/restart.sh"

echo ""
echo "========================================"
echo "  [2/2] 重启管理端全栈"
echo "========================================"
sh "${SCRIPT_DIR}/admin-full-stack/restart.sh"

echo ""
echo "========================================"
echo "  全部启动完成"
echo "========================================"
echo "  用户端前端：http://localhost:22345"
echo "  用户端后端：http://localhost:25050"
echo "  管理端前端：http://localhost:22346/login"
echo "  管理端后端：http://localhost:26060"
echo ""
echo "  日志目录：logs/"
echo "    用户端后端：logs/user-api.log"
echo "    用户端前端：logs/user-web.log"
echo "    管理端后端：logs/admin-api.log"
echo "    管理端前端：logs/admin-web.log"
