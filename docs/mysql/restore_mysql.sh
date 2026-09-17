#!/bin/bash
# MySQL 全库恢复脚本
# 用法:
#   ./restore_mysql.sh /root/mysql_backups/all_databases_20260917_113429.sql.gz   # 恢复指定备份
#   ./restore_mysql.sh                                                             # 恢复目录下最新的备份
#
# 运行时交互输入用户名和密码（密码不回显）。
# 如需免交互: MYSQL_USER=root MYSQL_PWD=密码 MYSQL_PORT=28080 ./restore_mysql.sh <备份文件>

set -euo pipefail

# ---------- 配置 ----------
BACKUP_DIR="/root/mysql_backups"      # 备份存放目录（用于默认取最新备份）
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-28080}"
LOG_PREFIX="[mysql-restore $(date '+%F %T')]"

# ---------- 凭据（优先环境变量，否则交互输入） ----------
if [ -z "${MYSQL_USER:-}" ]; then
  read -r -p "MySQL 用户名: " MYSQL_USER
fi
if [ -z "${MYSQL_PWD:-}" ]; then
  read -r -s -p "MySQL 密码: " MYSQL_PWD
  echo
fi
export MYSQL_PWD

if ! command -v mysql >/dev/null 2>&1; then
  echo "$LOG_PREFIX ERROR: mysql 客户端未安装"
  exit 1
fi

# ---------- 确定备份文件 ----------
BACKUP_FILE="${1:-}"
if [ -z "$BACKUP_FILE" ]; then
  BACKUP_FILE=$(ls -t "$BACKUP_DIR"/databases_*.sql.gz 2>/dev/null | head -1)
  if [ -z "$BACKUP_FILE" ]; then
    echo "$LOG_PREFIX ERROR: $BACKUP_DIR 下没有找到备份文件"
    exit 1
  fi
  echo "$LOG_PREFIX 未指定备份文件，使用最新的: $BACKUP_FILE"
fi

if [ ! -f "$BACKUP_FILE" ]; then
  echo "$LOG_PREFIX ERROR: 备份文件不存在: $BACKUP_FILE"
  exit 1
fi

# ---------- 展示备份内容并确认 ----------
echo "$LOG_PREFIX 备份文件: $BACKUP_FILE ($(du -h "$BACKUP_FILE" | awk '{print $1}'))"

if [[ "$BACKUP_FILE" == *.gz ]]; then
  DB_LIST=$(gzip -dc "$BACKUP_FILE" | grep -E '^CREATE DATABASE|^USE ' | head -30)
else
  DB_LIST=$(grep -E '^CREATE DATABASE|^USE ' "$BACKUP_FILE" | head -30)
fi
echo "$LOG_PREFIX 备份中包含的库:"
echo "$DB_LIST"

echo
read -r -p "确认要把该备份导入到 $MYSQL_HOST:$MYSQL_PORT 吗？目标库已有数据会被覆盖 [y/N]: " CONFIRM
if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
  echo "$LOG_PREFIX 已取消"
  exit 0
fi

# ---------- 解压（如需要） ----------
SQL_FILE="$BACKUP_FILE"
if [[ "$BACKUP_FILE" == *.gz ]]; then
  SQL_FILE="${BACKUP_FILE%.gz}"
  echo "$LOG_PREFIX 解压 -> $SQL_FILE"
  gunzip -kf "$BACKUP_FILE"
fi

# ---------- 导入 ----------
echo "$LOG_PREFIX 开始导入（库较大时需要等待）..."

if ! mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" \
    --default-character-set=utf8mb4 \
    < "$SQL_FILE" 2> "$SQL_FILE.restore.err"; then
  echo "$LOG_PREFIX ERROR: 导入失败，详见 $SQL_FILE.restore.err"
  exit 1
fi

# 无错误则删掉错误日志
if [ ! -s "$SQL_FILE.restore.err" ]; then
  rm -f "$SQL_FILE.restore.err"
fi

# ---------- 验证 ----------
echo "$LOG_PREFIX 导入完成，当前数据库列表:"
mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -e "SHOW DATABASES;"

echo "$LOG_PREFIX 完成"
