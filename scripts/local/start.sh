#!/bin/bash
# 启动爱创作原型本地预览服务器
# 用法：./start-prototype.sh

PORT=28585
DIR="$(cd "$(dirname "$0")/../.." && pwd)"

# 查找可用的 Python
if command -v python3 &> /dev/null; then
    PYTHON=python3
elif command -v python &> /dev/null; then
    PYTHON=python
else
    echo "错误：未找到 python3 或 python，请先安装 Python"
    exit 1
fi

echo "========================================"
echo "  爱创作原型预览服务器"
echo "========================================"
echo ""
echo "服务地址：http://localhost:${PORT}"
echo ""
echo "常用页面："
echo "  创作页：http://localhost:${PORT}/.superpowers/brainstorm/6491-1782131242/content/create.html"
echo "  首页：  http://localhost:${PORT}/.superpowers/brainstorm/6491-1782131242/content/index.html"
echo "  登录页：http://localhost:${PORT}/.superpowers/brainstorm/6491-1782131242/content/login.html"
echo ""
echo "按 Ctrl+C 停止服务"
echo ""

${PYTHON} -m http.server "${PORT}" --directory "${DIR}"
