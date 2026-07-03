#!/bin/bash
# 重启管理控制台前端开发服务器
# 用法：./restart.sh

cd "$(dirname "$0")" || exit 1
./stop.sh
./start.sh
