# 后端服务目录

## 目录说明

- `app.py`: Flask 主应用
- `requirements.txt`: Python 依赖包
- `library.db`: SQLite 数据库（运行时自动创建）
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
