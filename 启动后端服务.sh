#!/bin/bash
# Linux/macOS 启动脚本

echo ""
echo "╔════════════════════════════════════════════════════════╗"
echo "║     图书馆预约管理系统 v2.0 - 后端服务               ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

cd "$(dirname "$0")/backend"

if [ ! -d ".venv" ]; then
    echo "[1/3] 创建虚拟环境..."
    python3 -m venv .venv
fi

echo "[2/3] 激活虚拟环境..."
source .venv/bin/activate

echo "[3/3] 安装依赖..."
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

echo ""
echo "===================================="
echo "  启动后端服务..."
echo "===================================="
echo ""

python app.py
