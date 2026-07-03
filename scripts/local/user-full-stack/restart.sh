#!/bin/bash
# 重启用户端后端 + 前端
# 用法：./restart.sh

cd "$(dirname "$0")" || exit 1
./stop.sh
./start.sh