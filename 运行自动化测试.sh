#!/bin/bash
# Linux/macOS 自动化测试脚本

echo ""
echo "╔════════════════════════════════════════════════════════╗"
echo "║         图书馆预约管理系统 - 自动化测试             ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

# 检查 Python
if ! command -v python3 &> /dev/null; then
    echo "[错误] 未找到 Python，请先安装 Python 3.7+"
    exit 1
fi

# 安装测试依赖
echo "[1/3] 检查测试依赖..."
pip3 install colorama requests -q

echo ""
echo "[2/3] 运行系统测试..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
python3 test_system.py
SYSTEM_TEST_RESULT=$?

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 检查后端服务
echo "[3/3] 检查后端服务状态..."
if curl -s http://127.0.0.1:5000/health > /dev/null 2>&1; then
    echo "  后端服务运行中 ✓"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "[API测试] 运行后端 API 测试..."
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    cd backend
    python3 test_api.py
    API_TEST_RESULT=$?
    cd ..
else
    echo "  [警告] 后端服务未运行，跳过 API 测试"
    echo "  [提示] 启动后端服务后可运行: python3 backend/test_api.py"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "✅ 测试完成"
echo ""

if [ $SYSTEM_TEST_RESULT -eq 0 ]; then
    echo "  系统测试: 通过 ✓"
else
    echo "  系统测试: 失败 ✗"
fi

if [ -n "$API_TEST_RESULT" ]; then
    if [ $API_TEST_RESULT -eq 0 ]; then
        echo "  API 测试: 通过 ✓"
    else
        echo "  API 测试: 失败 ✗"
    fi
else
    echo "  API 测试: 未运行 -"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
