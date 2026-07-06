# 后端服务目录

## 目录说明

- `app.py`: Flask 主应用
- `requirements.txt`: Python 依赖包
- `.env.example`: 数据库配置模板（复制为 `.env` 并填写 MySQL 连接信息）
- `init_db.sql`: MySQL 建库脚本（仅建库，表由 SQLAlchemy 自动创建）
- `.venv/`: Python 虚拟环境（自动创建）

## 模块说明

### database/
数据库配置和初始化

### models/
数据模型定义（账号、预约记录）

### routes/
RESTful API 路由

### utils/
工具函数（Token 解析、图书馆 API 封装）

## 数据库配置（MySQL）

启动前先配置数据库连接：

1. 复制配置模板并填写 MySQL 密码：
   ```bash
   copy .env.example .env      # Windows
   cp .env.example .env        # Linux/macOS
   ```
2. 创建数据库（表结构由后端启动时自动创建）：
   ```bash
   mysql -u root -p < init_db.sql
   ```

`.env` 支持的配置项：

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DB_TYPE` | 数据库类型：`mysql` 或 `sqlite` | `mysql` |
| `DB_HOST` | MySQL 主机 | `127.0.0.1` |
| `DB_PORT` | MySQL 端口 | `3306` |
| `DB_USER` | 用户名 | `root` |
| `DB_PASSWORD` | 密码 | 空 |
| `DB_NAME` | 数据库名 | `library_reservation` |
| `DB_CHARSET` | 字符集 | `utf8mb4` |

## 启动服务

Windows:
```bash
.venv\Scripts\activate
python app.py
```

Linux/macOS:
```bash
source .venv/bin/activate
python app.py
```

服务地址: http://127.0.0.1:5000
