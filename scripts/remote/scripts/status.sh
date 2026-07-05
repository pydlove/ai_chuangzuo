#!/bin/bash
# 查看爱创作服务状态

echo "========================================"
echo "爱创作服务状态"
echo "========================================"

systemctl status aichuangzuo-user-api --no-pager 2>/dev/null || echo "用户端后端: 未安装或状态异常"

if systemctl list-unit-files | grep -q "^aichuangzuo-admin-api"; then
    echo ""
    systemctl status aichuangzuo-admin-api --no-pager 2>/dev/null || echo "管理端后端: 未安装或状态异常"
fi

echo ""
echo "端口监听情况:"
ss -tlnp 2>/dev/null | grep -E ":22345|:22346|:22347|:22348" || netstat -tlnp 2>/dev/null | grep -E ":22345|:22346|:22347|:22348" || true

echo ""
echo "访问地址:"
echo "  用户端: http://$(hostname -I | awk '{print $1}'):22345"
echo "  管理端: http://$(hostname -I | awk '{print $1}'):22347"
