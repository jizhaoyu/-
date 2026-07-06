#!/bin/bash
# Linux/macOS 一键启动全部服务（后端 + Vue3 前端）

echo ""
echo "╔════════════════════════════════════════════════════════╗"
echo "║     图书馆预约管理系统 v2.0 - 一键启动全部服务      ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

ROOT="$(cd "$(dirname "$0")" && pwd)"

# ==================== 后端服务（后台） ====================
echo "[1/2] 启动后端服务..."
(
    cd "$ROOT/backend" || exit 1
    if [ ! -d ".venv" ]; then
        python3 -m venv .venv
    fi
    source .venv/bin/activate
    pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
    python app.py
) &
BACKEND_PID=$!

# 退出时清理后端进程
cleanup() {
    echo ""
    echo "正在停止后端服务 (PID $BACKEND_PID)..."
    kill "$BACKEND_PID" 2>/dev/null
    exit 0
}
trap cleanup INT TERM

# 等待后端就绪
sleep 5

# ==================== Vue3 前端（前台） ====================
echo ""
echo "[2/2] 启动 Vue3 前端..."
echo ""
echo "===================================="
echo "  后端服务: http://127.0.0.1:5000"
echo "  前端界面: http://127.0.0.1:8080"
echo "  API 代理: /api -> http://127.0.0.1:5000"
echo "===================================="
echo "  按 Ctrl+C 停止全部服务"
echo "===================================="
echo ""

cd "$ROOT/frontend-vue" || exit 1
if [ ! -d "node_modules" ]; then
    npm install || npm install --registry=https://registry.npmmirror.com
fi
npm run dev

# 前端退出后一并清理后端
cleanup
