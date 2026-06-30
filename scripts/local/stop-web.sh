#!/bin/bash
lsof -ti:22345 | xargs kill -9 2>/dev/null
echo "端口 22345 已关闭"
