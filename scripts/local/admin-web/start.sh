#!/bin/bash
# 启动管理控制台前端开发服务器
# 用法：./start.sh

cd "$(dirname "$0")/../../../project/admin/web" || exit 1
npm run dev -- --host
