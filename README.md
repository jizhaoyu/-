# 图书馆预约管理系统 v2.0

> 前后端分离架构 · Vue3 + Vite 前端 · Python Flask 后端 · MySQL 数据库

## 📋 项目简介

图书馆座位预约管理系统，采用前后端分离架构，支持多账号管理、Token 自动监控、一键预约等功能。

## ✨ 主要特性

- ✅ **前后端分离**：Vue3 + Vite 前端 + Python Flask 后端
- ✅ **数据库存储**：使用 MySQL 持久化账号与预约数据（可回退 SQLite）
- ✅ **GSAP 动画**：流畅的页面交互动画效果
- ✅ **Token 管理**：自动解析 JWT、实时监控过期状态
- ✅ **多账号支持**：可管理多个图书馆账号
- ✅ **智能预约**：支持"第一个空闲"和"随机选择"策略
- ✅ **完整功能**：预约、签到、签退、取消等全流程支持

## 🏗️ 项目结构

```
QFNULibraryBook-main/
├── backend/                       # 后端服务（Flask）
│   ├── app.py                     # Flask 主程序
│   ├── requirements.txt           # Python 依赖
│   ├── .env.example               # 数据库配置模板（复制为 .env）
│   ├── init_db.sql                # MySQL 建库脚本
│   ├── database/db.py             # 数据库初始化
│   ├── models/account.py          # 账号和预约记录模型
│   ├── routes/                    # API 路由
│   │   ├── account_routes.py      # 账号管理 API
│   │   └── reservation_routes.py  # 预约管理 API
│   └── utils/                     # 工具函数
│       ├── token_utils.py         # Token 解析和验证
│       └── library_api.py         # 图书馆 API 封装
├── frontend-vue/                  # 前端界面（Vue3 + Vite）
│   ├── src/                       # 源码（组件、路由、状态、API）
│   ├── index.html                 # 入口 HTML
│   ├── package.json               # 前端依赖与脚本
│   └── vite.config.ts             # Vite 配置（端口 8080，/api 代理到 5000）
├── json/seat_info/                # 座位信息数据
├── assets/                        # 资源文件
├── 启动全部服务.bat / .sh          # 一键启动后端 + Vue3 前端
├── 快速启动指南.md                 # 快速上手文档
├── 项目结构说明.md                 # 结构与开发说明
└── API文档.md                      # 接口文档
```

## 🚀 快速开始

### 环境要求

- **Python**: 3.7+
- **Node.js**: 16+（含 npm）
- **MySQL**: 5.7+ / 8.0+
- **浏览器**: Chrome/Edge/Firefox 最新版

### 1️⃣ 准备 MySQL 数据库

先创建数据库（表结构由后端首次启动时自动创建）：

```bash
mysql -u root -p < backend/init_db.sql
```

再复制配置模板并填入你的数据库账号密码：

```bash
cd backend
cp .env.example .env      # Windows: copy .env.example .env
# 编辑 .env，修改 DB_USER / DB_PASSWORD 等
```

`.env` 关键配置：

```env
DB_TYPE=mysql
DB_HOST=127.0.0.1
DB_PORT=3306
DB_USER=root
DB_PASSWORD=你的密码
DB_NAME=library_reservation
```

> 想临时用免安装的 SQLite？把 `DB_TYPE` 改成 `sqlite` 即可，其余配置忽略。

### 2️⃣ 一键启动全部服务

双击 `启动全部服务.bat`（Windows）或运行 `启动全部服务.sh`（macOS/Linux），脚本会自动：

- 创建后端虚拟环境、安装 Python 依赖、初始化数据库并在新窗口启动 Flask 服务（http://127.0.0.1:5000）
- 安装前端 npm 依赖并启动 Vite 开发服务器（http://127.0.0.1:8080）

手动启动（可选）：

```bash
# 后端
cd backend
python -m venv .venv
.venv\Scripts\activate       # Windows
# source .venv/bin/activate  # macOS/Linux
pip install -r requirements.txt
python app.py

# 前端（另开一个终端）
cd frontend-vue
npm install
npm run dev
```

### 3️⃣ 打开浏览器

访问 http://127.0.0.1:8080 即可使用系统。前端通过 Vite 的 `/api` 代理访问后端，无需额外配置跨域。

## 📖 使用说明

### 获取 Token

1. 浏览器打开图书馆网站：http://libyy.qfnu.edu.cn/h5/index.html
2. 完成登录（包括滑块验证）
3. 按 F12 打开开发者工具 → Console（控制台）
4. 执行命令：
   ```javascript
   localStorage.getItem('token')
   localStorage.getItem('name')
   ```
5. 复制输出的 Token 和姓名

### 添加账号

在左侧表单填写姓名、Token（支持带 `bearer` 前缀）、区域 ID（默认 22）、预约日期、座位策略，点击"添加账号"。

### 预约操作

- **测试预约**：验证是否有可用座位，不实际提交
- **执行预约**：提交正式预约请求
- **签到 / 签退**：对已预约座位进行签到、签退
- **取消预约**：取消当前预约

## 🔧 API 文档

### 账号管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/accounts` | GET | 获取所有账号 |
| `/api/accounts` | POST | 添加账号 |
| `/api/accounts/{id}` | GET | 获取账号详情 |
| `/api/accounts/{id}` | PUT | 更新账号 |
| `/api/accounts/{id}` | DELETE | 删除账号 |

### 预约管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/reservations/test` | POST | 测试预约 |
| `/api/reservations/execute` | POST | 执行预约 |
| `/api/reservations/check-in` | POST | 签到 |
| `/api/reservations/sign-out` | POST | 签退 |
| `/api/reservations/current` | POST | 获取当前预约 |
| `/api/reservations/cancel` | POST | 取消预约 |
| `/api/reservations/history` | GET | 获取历史记录 |

完整接口说明见 [API文档.md](API文档.md)。

## 🎨 技术栈

**后端**：Flask 3.0 · SQLAlchemy · PyMySQL（MySQL 驱动）· Flask-CORS · Requests · Cryptography（AES 加密）· python-dotenv

**前端**：Vue 3.4 · Vite 5 · Vue Router · Pinia · Arco Design · Axios · GSAP 3.12 · dayjs

## ⚠️ 注意事项

1. **数据库配置**：首次运行前须先建库并配置 `backend/.env`，`.env` 含密码不会提交到 Git
2. **Token 有效期**：约 100 分钟，过期后需重新获取
3. **端口占用**：确保 5000（后端）和 8080（前端）端口未被占用
4. **网络连接**：后端需能访问图书馆服务器（http://libyy.qfnu.edu.cn）

## 📄 许可证

本项目采用 MIT License，详见 [LICENSE](LICENSE)。
