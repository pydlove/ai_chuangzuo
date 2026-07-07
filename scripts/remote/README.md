# 爱创作 - 远程部署脚本

## 文件说明

```
scripts/remote/
├── deploy.sh                        # 主部署脚本
├── .env.example                     # 环境变量模板
├── systemd/
│   ├── aichuangzuo-user-api.service # 用户端后端 systemd 服务
│   └── aichuangzuo-admin-api.service# 管理端后端 systemd 服务
└── scripts/
    ├── start-all.sh                 # 服务器端启动所有服务
    ├── stop-all.sh                  # 服务器端停止所有服务
    ├── restart-all.sh               # 服务器端重启所有服务
    └── status.sh                    # 查看服务状态
```

## 使用前准备

### 1. 本地环境

- Java 17
- Maven 3.8+
- Node.js 18+
- sshpass（使用密码登录时）或 SSH 密钥

### 2. 服务器环境

- Linux（CentOS 7+/Ubuntu 18+）
- Java 17
- Nginx
- MySQL 8
- systemd

### 3. 配置脚本

编辑 `deploy.sh` 顶部的配置区：

```bash
SERVER_IP="你的服务器IP"
SERVER_USER="root"
SERVER_PASSWORD="你的密码"          # 或留空使用 SSH_KEY_PATH
SSH_KEY_PATH="~/.ssh/id_rsa"      # 如使用密钥登录
SERVER_DOMAIN="yourdomain.com"    # 或 localhost
```

### 4. 配置环境变量

```bash
cp scripts/remote/.env.example scripts/remote/.env
# 编辑 .env 文件，填入 Jasypt 密钥、数据库密码等
```

## Nginx 配置

部署脚本会把项目同级的 `nginx/nginx.conf` 上传到服务器的 `/etc/nginx/nginx.conf`，并执行 `nginx -t` 和 `nginx -s reload`。

该 `nginx.conf` 中已经包含爱创作的配置：

- 用户端前端: `listen 22345`
- 用户端后端 upstream: `127.0.0.1:22346`
- 管理端前端: `listen 22347`
- 管理端后端 upstream: `127.0.0.1:22348`

如果你需要绑定域名或开启 HTTPS，直接修改 `nginx/nginx.conf` 中对应的 `server_name` 和 SSL 配置，然后重新部署即可。

## 部署

```bash
cd scripts/remote
./deploy.sh
```

部署完成后：

- 用户端: http://服务器IP:22345
- 管理端: http://服务器IP:22347
- 用户端后端: http://服务器IP:22346
- 管理端后端: http://服务器IP:22348

## 服务器端常用命令

部署脚本会把启停脚本复制到服务器 `/root/app/aichuangzuo/` 下：

```bash
# 查看状态
/root/app/aichuangzuo/status.sh

# 重启所有服务
/root/app/aichuangzuo/restart-all.sh

# 停止所有服务
/root/app/aichuangzuo/stop-all.sh

# 启动所有服务
/root/app/aichuangzuo/start-all.sh
```

## 注意事项

1. 首次部署前，请确保 MySQL 中已创建数据库，且 `application-prod.yml` 中的数据库连接信息正确。
2. 如果使用 HTTPS，请配置 `NGINX_SSL_CERT` 和 `NGINX_SSL_KEY`，并修改生成的 nginx 配置添加 SSL。
3. 管理端后端 `project/admin/api` 目前为空，脚本会自动检测并跳过；添加代码后自动部署。
