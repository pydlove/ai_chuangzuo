#!/bin/bash
# 远程执行公共环境变量与命令包装。
# 当本地存在 systemctl 时（即已在 Linux 服务器上），命令直接在本地执行；
# 否则（macOS 等本地开发机）通过 SSH 在配置的目标服务器上执行。

# 服务器连接信息，优先从环境变量读取（由管理端升级管理页面注入）
export SERVER_IP="${SERVER_IP:-101.126.15.58}"
export SERVER_USER="${SERVER_USER:-root}"
export SERVER_PASSWORD="${SERVER_PASSWORD:-}"
export SSH_KEY_PATH="${SSH_KEY_PATH:-~/.ssh/id_rsa}"

if [ -z "$SERVER_IP" ]; then
  echo "[ERROR] SERVER_IP 未配置" >&2
  exit 1
fi

SSH_MUX_PATH="/tmp/ssh_mux_${SERVER_IP}_${SERVER_USER}"
SSH_MUX_OPTS="-o ControlMaster=auto -o ControlPath=$SSH_MUX_PATH -o ControlPersist=600"

if [ -n "$SSH_KEY_PATH" ] && [ -f "$SSH_KEY_PATH" ]; then
  SSH_CMD="ssh -i $SSH_KEY_PATH -o StrictHostKeyChecking=no $SSH_MUX_OPTS"
else
  if ! command -v sshpass &> /dev/null; then
    echo "[ERROR] 未安装 sshpass，且未配置有效 SSH 密钥。" >&2
    echo "        macOS 请执行: brew install sshpass" >&2
    echo "        或在升级管理页面配置 SSH 密钥路径。" >&2
    exit 1
  fi
  SSH_CMD="sshpass -p '$SERVER_PASSWORD' ssh -o StrictHostKeyChecking=no -o ConnectTimeout=30 -o ServerAliveInterval=30 -o ServerAliveCountMax=3 $SSH_MUX_OPTS"
fi

REMOTE_HOST="$SERVER_USER@$SERVER_IP"

# 如果存在旧的/失效的 SSH mux socket，先清理，避免复用时挂起
if [ -S "$SSH_MUX_PATH" ]; then
  $SSH_CMD -O check "$REMOTE_HOST" 2>/dev/null || rm -f "$SSH_MUX_PATH"
fi

# 在本地或远程执行一条命令（建议以整串字符串传入）
run_cmd() {
  if command -v systemctl &> /dev/null; then
    # 已经在服务器上，直接本地执行
    eval "$@"
  else
    # 本地开发机，通过 SSH 在目标服务器执行
    $SSH_CMD "$REMOTE_HOST" "$@"
  fi
}

# 清理 SSH 连接复用的 master 连接（可选）
close_ssh_mux() {
  $SSH_CMD -O exit "$REMOTE_HOST" 2>/dev/null || true
  rm -f "$SSH_MUX_PATH"
}
