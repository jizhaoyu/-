#!/bin/bash
# Linux/macOS 启动脚本

echo ""
echo "╔════════════════════════════════════════════════════════╗"
echo "║     图书馆预约管理系统 v2.0 - Vue3 前端界面          ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

cd "$(dirname "$0")/frontend-vue"

if [ ! -d "node_modules" ]; then
    echo "[1/2] 安装依赖..."
    npm install
fi

echo ""
echo "[2/2] 启动开发服务器..."
echo ""
echo "===================================="
echo "  Vue3 前端: http://127.0.0.1:8080"
echo "  API 代理: http://127.0.0.1:5000"
echo "===================================="
echo ""

npm run dev
