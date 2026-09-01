#!/bin/bash
# 查看爱创作服务状态

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/../lib/_remote-env.sh"

echo "========================================"
echo "爱创作服务状态"
echo "========================================"

run_cmd "systemctl status aichuangzuo-user-api --no-pager 2>/dev/null || echo '用户端后端: 未安装或状态异常'"

if run_cmd "systemctl list-unit-files | grep -q '^aichuangzuo-admin-api'"; then
    echo ""
    run_cmd "systemctl status aichuangzuo-admin-api --no-pager 2>/dev/null || echo '管理端后端: 未安装或状态异常'"
fi

echo ""
echo "端口监听情况:"
run_cmd "ss -tlnp 2>/dev/null | grep -E ':22345|:22347|:25050|:26060' || netstat -tlnp 2>/dev/null | grep -E ':22345|:22347|:25050|:26060' || true"

echo ""
echo "访问地址:"
echo "  用户端: http://${SERVER_IP}:22345"
echo "  管理端: http://${SERVER_IP}:22347"
