#!/bin/bash
# MySQL 全库备份脚本
# 用法: 上传到服务器（如 /root/scripts/），chmod +x backup_mysql.sh 后执行
#       建议加入 crontab: 0 3 * * * /root/scripts/backup_mysql.sh >> /var/log/mysql_backup.log 2>&1
#
# 运行时交互输入用户名和密码（密码不回显）。
# 如需免交互（如 crontab），改用环境变量: MYSQL_USER=root MYSQL_PWD=密码 /root/scripts/backup_mysql.sh

set -euo pipefail

# ---------- 配置 ----------
BACKUP_DIR="/root/mysql_backups"      # 备份存放目录
KEEP_DAYS=7                           # 保留最近 N 天的备份
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
LOG_PREFIX="[mysql-backup $(date '+%F %T')]"

# ---------- 凭据（优先环境变量，否则交互输入） ----------
if [ -z "${MYSQL_USER:-}" ]; then
  read -r -p "MySQL 用户名: " MYSQL_USER
fi
if [ -z "${MYSQL_PWD:-}" ]; then
  read -r -s -p "MySQL 密码: " MYSQL_PWD
  echo
fi
export MYSQL_PWD

if ! command -v mysqldump >/dev/null 2>&1; then
  echo "$LOG_PREFIX ERROR: mysqldump 未安装"
  exit 1
fi

mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
BACKUP_FILE="$BACKUP_DIR/databases_$TIMESTAMP.sql.gz"

# ---------- 备份业务数据库（排除系统库） ----------
# 系统库 mysql/sys/information_schema/performance_schema 不备份，
# 避免恢复时覆盖目标服务器的用户表和权限
DBS=$(mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -N -e \
  "SHOW DATABASES;" | grep -Ev '^(mysql|sys|information_schema|performance_schema)$')

if [ -z "$DBS" ]; then
  echo "$LOG_PREFIX ERROR: 没有找到可备份的业务数据库"
  exit 1
fi

echo "$LOG_PREFIX 本次备份的数据库:"
echo "$DBS" | sed "s/^/$LOG_PREFIX   /"

echo "$LOG_PREFIX 开始备份 -> $BACKUP_FILE"

if ! mysqldump -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" \
    --databases $DBS \
    --single-transaction \
    --routines --triggers --events \
    --set-gtid-purged=OFF \
    2> "$BACKUP_DIR/backup_$TIMESTAMP.err" \
    | gzip > "$BACKUP_FILE"; then
  echo "$LOG_PREFIX ERROR: 备份失败，详见 $BACKUP_DIR/backup_$TIMESTAMP.err"
  rm -f "$BACKUP_FILE"
  exit 1
fi

# 无错误则删掉错误日志
if [ ! -s "$BACKUP_DIR/backup_$TIMESTAMP.err" ]; then
  rm -f "$BACKUP_DIR/backup_$TIMESTAMP.err"
fi

BACKUP_SIZE=$(du -h "$BACKUP_FILE" | awk '{print $1}')
echo "$LOG_PREFIX 备份完成，大小: $BACKUP_SIZE"

# ---------- 清理过期备份 ----------
DELETED=$(find "$BACKUP_DIR" -name 'databases_*.sql.gz' -mtime +$KEEP_DAYS -delete -print | wc -l)
[ "$DELETED" -gt 0 ] && echo "$LOG_PREFIX 清理了 $DELETED 个过期备份"

echo "$LOG_PREFIX 完成"
