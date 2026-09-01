#!/bin/bash
# 停止所有爱创作服务

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/../lib/_remote-env.sh"

echo "[INFO] 停止爱创作所有服务..."

run_cmd "systemctl stop aichuangzuo-user-api 2>/dev/null || true"
run_cmd "systemctl stop aichuangzuo-admin-api 2>/dev/null || true"

echo "[INFO] 服务已停止"
