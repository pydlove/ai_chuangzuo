# 项目目录结构规范

本文档定义本项目的目录组织规范，所有新代码、新模块、新脚本都应遵循此规范。

## 顶层目录

```
project-root/
├── scripts/            # 脚本
├── project/            # 业务代码
├── config/             # 环境配置
├── docs/               # 文档
├── tests/              # 测试
├── data/               # 本地数据与测试数据
├── logs/               # 本地运行日志
├── .gitignore
├── Makefile
└── README.md
```

## 目录说明

### scripts/ — 脚本

按运行环境分为两类：

- `scripts/local/`：本地开发相关脚本，如 `start.sh`、`stop.sh`、`setup.sh`。
- `scripts/remote/`：远程部署运维脚本，如 `deploy.sh`、`restart.sh`、`rollback.sh`。

命名约定：同功能脚本尽量同名或语义对齐，便于记忆。

### project/ — 业务代码

按业务端拆分，每个端再分前后台：

- `project/user/web/`：用户端前端
- `project/user/api/`：用户端后端
- `project/admin/web/`：管理端前端
- `project/admin/api/`：管理端后端
- `project/shared/`：用户端与管理端共用代码，如类型定义、工具函数、常量

每个端可以独立开发、独立构建、独立部署。

### config/ — 环境配置

- `config/local.env`：本地开发环境变量
- `config/remote.env`：远程/生产环境变量
- `config/example.env`：环境变量模板，唯一提交到仓库的 env 文件

所有 `*.env` 文件（除 `example.env` 外）都不得提交到 Git。

### docs/ — 文档

- `docs/api/`：接口文档
- `docs/ops/`：运维/部署文档
- `docs/architecture/`：架构设计文档

### tests/ — 测试

- `tests/e2e/`：端到端测试
- `tests/integration/`：集成测试

### data/ — 本地数据

- `data/fixtures/`：测试数据、Mock 数据
- `data/local-db/`：本地数据库文件

### logs/ — 本地日志

存放本地运行产生的日志文件，不提交到 Git。

## Git 忽略规则

```gitignore
# 环境变量
*.env
!config/example.env

# 日志
logs/*
!logs/.gitkeep

# 本地数据
data/local-db/*
!data/local-db/.gitkeep
```

## 新增目录流程

1. 判断新增内容属于哪个业务端（user / admin / shared）。
2. 判断其职责（web / api / 工具 / 配置 / 文档 / 测试 / 数据）。
3. 放入对应目录，不得在根目录随意新建目录。
4. 如目录为空，添加 `.gitkeep` 以纳入版本管理。
