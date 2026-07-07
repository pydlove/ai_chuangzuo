#!/bin/bash
# 启动所有爱创作服务

set -e

echo "[INFO] 启动爱创作所有服务..."

# 首次启动时初始化热搜抓取环境（Chrome 路径探测 + DLP CA truststore 生成）
APP_DIR="/root/app/aichuangzuo"
if [ -x "${APP_DIR}/scripts/setup-hotsearch-env.sh" ]; then
    "${APP_DIR}/scripts/setup-hotsearch-env.sh"
fi

systemctl start aichuangzuo-user-api
if systemctl list-unit-files | grep -q "^aichuangzuo-admin-api"; then
    systemctl start aichuangzuo-admin-api
fi

nginx -s reload 2>/dev/null || nginx

echo "[INFO] 服务启动完成"
echo "  用户端: http://$(hostname -I | awk '{print $1}'):22345"
echo "  管理端: http://$(hostname -I | awk '{print $1}'):22347"
