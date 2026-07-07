#!/bin/bash
# 停止所有爱创作服务

set -e

echo "[INFO] 停止爱创作所有服务..."

systemctl stop aichuangzuo-user-api 2>/dev/null || true
systemctl stop aichuangzuo-admin-api 2>/dev/null || true

echo "[INFO] 服务已停止"
