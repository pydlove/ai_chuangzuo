#!/bin/bash
# 重启管理端后端 + 前端
# 用法：./restart.sh

cd "$(dirname "$0")" || exit 1
./stop.sh
./start.sh
