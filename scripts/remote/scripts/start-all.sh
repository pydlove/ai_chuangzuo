#!/bin/bash
# 启动所有爱创作服务

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/../lib/_remote-env.sh"

echo "[INFO] 启动爱创作所有服务..."

# 首次启动时初始化热搜抓取环境（Chrome 路径探测 + DLP CA truststore 生成）
APP_DIR="/root/app/aichuangzuo"
if run_cmd "[ -x '${APP_DIR}/scripts/setup-hotsearch-env.sh' ]"; then
    run_cmd "${APP_DIR}/scripts/setup-hotsearch-env.sh"
fi

run_cmd "systemctl start aichuangzuo-user-api"
if run_cmd "systemctl list-unit-files | grep -q '^aichuangzuo-admin-api'"; then
    run_cmd "systemctl start aichuangzuo-admin-api"
fi

run_cmd "nginx -s reload 2>/dev/null || nginx"

echo "[INFO] 服务启动完成"
echo "  用户端: http://${SERVER_IP}:22345"
echo "  管理端: http://${SERVER_IP}:22347"
