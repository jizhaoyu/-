# 图书馆预约管理系统 v2.0

> 前后端分离架构 · SQLite 数据库 · GSAP 动画 · Python Flask 后端

## 📋 项目简介

这是一个全新重构的图书馆座位预约管理系统，采用前后端分离架构，支持多账号管理、Token 自动监控、一键预约等功能。

## ✨ 主要特性

- ✅ **前后端分离**：前端 HTML/CSS/JS + 后端 Python Flask
- ✅ **数据库存储**：使用 SQLite 持久化账号数据
- ✅ **GSAP 动画**：流畅的页面交互动画效果
- ✅ **Token 管理**：自动解析 JWT、实时监控过期状态
- ✅ **多账号支持**：可管理多个图书馆账号
- ✅ **智能预约**：支持"第一个空闲"和"随机选择"策略
- ✅ **完整功能**：预约、签到、签退、取消等全流程支持

## 🏗️ 项目结构

```
QFNULibraryBook-main/
├── backend/                    # 后端服务
│   ├── app.py                 # Flask 主程序
│   ├── requirements.txt       # Python 依赖
│   ├── library.db            # SQLite 数据库（自动创建）
│   ├── database/             # 数据库模块
│   │   └── db.py            # 数据库初始化
│   ├── models/               # 数据模型
│   │   └── account.py       # 账号和预约记录模型
│   ├── routes/               # API 路由
│   │   ├── account_routes.py    # 账号管理 API
│   │   └── reservation_routes.py # 预约管理 API
│   └── utils/                # 工具函数
│       ├── token_utils.py   # Token 解析和验证
│       └── library_api.py   # 图书馆 API 封装
├── frontend/                  # 前端界面
│   ├── index.html            # 主页面
│   ├── css/
│   │   └── style.css        # 样式表（含响应式）
│   └── js/
│       └── app.js           # 前端逻辑（含 GSAP 动画）
├── 启动后端服务.bat           # 后端启动脚本
├── 启动前端界面.bat           # 前端启动脚本
└── README_V2.md              # 本文档
```

## 🚀 快速开始

### 环境要求

- **Python**: 3.7+
- **浏览器**: Chrome/Edge/Firefox 最新版

### 安装步骤

#### 1️⃣ 启动后端服务

双击 `启动后端服务.bat`，脚本会自动：
- 创建 Python 虚拟环境
- 安装所需依赖
- 初始化数据库
- 启动 Flask 服务（http://127.0.0.1:5000）

**手动启动（可选）**：
```bash
cd backend
python -m venv .venv
.venv\Scripts\activate    # Windows
# source .venv/bin/activate  # macOS/Linux
pip install -r requirements.txt
python app.py
```

#### 2️⃣ 启动前端界面

双击 `启动前端界面.bat`，会启动简单 HTTP 服务器（http://127.0.0.1:8080）

**手动启动（可选）**：
```bash
cd frontend
python -m http.server 8080
```

#### 3️⃣ 打开浏览器

访问 http://127.0.0.1:8080 即可使用系统。

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

1. 在左侧表单填写：
   - **姓名**：你的真实姓名
   - **Token**：从浏览器复制的完整 token（支持带 `bearer` 前缀）
   - **区域 ID**：默认 22（可根据实际修改）
   - **预约日期**：今天/明天
   - **座位策略**：第一个空闲/随机选择

2. 点击"添加账号"

### 管理账号

- **查看详情**：点击右侧列表中的账号卡片
- **编辑账号**：在详情面板点击"编辑"
- **删除账号**：在详情面板点击"删除"

### 预约操作

- **测试预约**：验证是否有可用座位，不实际提交
- **执行预约**：提交正式预约请求
- **签到**：对已预约的座位进行签到
- **删除**：删除当前账号

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

## 🗄️ 数据库设计

### accounts 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 主键 |
| name | VARCHAR(50) | 姓名 |
| token | TEXT | JWT Token |
| area_id | VARCHAR(20) | 区域 ID |
| date_preference | VARCHAR(20) | 日期偏好 |
| strategy | VARCHAR(20) | 座位策略 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### reservations 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 主键 |
| account_id | INTEGER | 关联账号 ID |
| reservation_id | VARCHAR(50) | 服务器预约 ID |
| seat_no | VARCHAR(50) | 座位号 |
| seat_id | VARCHAR(50) | 座位 ID |
| area_name | VARCHAR(100) | 区域名称 |
| segment_id | VARCHAR(50) | 时间段 ID |
| reserve_date | VARCHAR(20) | 预约日期 |
| start_time | VARCHAR(20) | 开始时间 |
| end_time | VARCHAR(20) | 结束时间 |
| status | VARCHAR(20) | 状态 |
| message | TEXT | 响应消息 |
| created_at | DATETIME | 创建时间 |

## 🎨 技术栈

### 后端
- **Flask 3.0**: Web 框架
- **SQLAlchemy**: ORM 数据库操作
- **Flask-CORS**: 跨域支持
- **Requests**: HTTP 请求
- **Cryptography**: AES 加密

### 前端
- **原生 HTML/CSS/JS**: 无框架依赖
- **GSAP 3.12**: 高性能动画库
- **Fetch API**: 异步数据请求
- **CSS Grid/Flexbox**: 现代布局

## ⚠️ 注意事项

1. **Token 有效期**：约 100 分钟，过期后需重新获取
2. **端口占用**：确保 5000（后端）和 8080（前端）端口未被占用
3. **网络连接**：后端需能访问图书馆服务器（http://libyy.qfnu.edu.cn）
4. **浏览器兼容**：推荐使用 Chrome/Edge 最新版

## 🔄 与旧版本的区别

| 特性 | 旧版本 | v2.0 |
|------|--------|------|
| 架构 | 单页面 + localStorage | 前后端分离 + 数据库 |
| 数据存储 | 浏览器本地存储 | SQLite 数据库 |
| Token 管理 | 手动复制文件 | Web 界面管理 |
| 动画效果 | 纯 CSS | GSAP 专业动画 |
| API 封装 | 分散在多个脚本 | 统一 RESTful API |
| 预约记录 | 不保存 | 数据库持久化 |

## 📝 开发说明

### 添加新功能

1. **后端 API**：在 `backend/routes/` 下添加路由
2. **数据模型**：在 `backend/models/` 下定义模型
3. **前端界面**：修改 `frontend/` 下的文件
4. **动画效果**：在 `app.js` 中使用 GSAP

### 调试

- **后端日志**：查看终端输出
- **前端调试**：浏览器 F12 → Console
- **数据库查看**：使用 DB Browser for SQLite 打开 `backend/library.db`

## 📄 许可证

本项目继承原项目的 MIT License。

## 🙏 致谢

- 原项目：QFNULibraryBook
- 动画库：GSAP (GreenSock)
- Web 框架：Flask

---

**版本**: 2.0  
**更新日期**: 2026-07-05  
**作者**: v0-ctf (基于原项目重构)
