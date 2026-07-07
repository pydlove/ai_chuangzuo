#!/bin/bash
# 启动用户端前端开发服务器
# 用法：./start.sh

cd "$(dirname "$0")/../../../project/user/web" || exit 1
npm run dev -- --host
