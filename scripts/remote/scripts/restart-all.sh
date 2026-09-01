#!/bin/bash
# 重启所有爱创作服务

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/../lib/_remote-env.sh"

bash "$SCRIPT_DIR/stop-all.sh"
sleep 2
bash "$SCRIPT_DIR/start-all.sh"

close_ssh_mux
