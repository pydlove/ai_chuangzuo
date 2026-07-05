#!/bin/bash
# 启动所有爱创作服务

set -e

echo "[INFO] 启动爱创作所有服务..."

systemctl start aichuangzuo-user-api
if systemctl list-unit-files | grep -q "^aichuangzuo-admin-api"; then
    systemctl start aichuangzuo-admin-api
fi

nginx -s reload 2>/dev/null || nginx

echo "[INFO] 服务启动完成"
echo "  用户端: http://$(hostname -I | awk '{print $1}'):22345"
echo "  管理端: http://$(hostname -I | awk '{print $1}'):22347"
