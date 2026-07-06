#!/bin/bash
# Linux/macOS 项目清理脚本

echo ""
echo "╔════════════════════════════════════════════════════════╗"
echo "║         项目清理工具 - 移除旧版本文件               ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""
echo "[警告] 此脚本将移动以下旧版本文件到 _archive 目录:"
echo "  - py/ 目录 (旧版本 Python 脚本)"
echo "  - web/ 目录 (旧版本 Web 文件)"
echo "  - v3.1/ 目录 (历史版本)"
echo "  - 旧的启动脚本"
echo "  - 旧的文档"
echo ""
echo "保留的文件:"
echo "  - backend/ (新后端)"
echo "  - frontend/ (新原生前端)"
echo "  - frontend-vue/ (新Vue3前端)"
echo "  - 所有新的启动脚本和文档"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

read -p "确认清理旧文件? (输入 YES 继续): " confirm
if [ "$confirm" != "YES" ]; then
    echo ""
    echo "已取消清理操作"
    exit 0
fi

cd "$(dirname "$0")"

echo ""
echo "[1/4] 创建归档目录..."
mkdir -p "_archive/old_versions"

echo ""
echo "[2/4] 移动旧版本目录..."
if [ -d "py" ]; then
    mv "py" "_archive/old_versions/"
    echo "  ✓ 已移动 py/"
fi
if [ -d "web" ]; then
    mv "web" "_archive/old_versions/"
    echo "  ✓ 已移动 web/"
fi
if [ -d "v3.1" ]; then
    mv "v3.1" "_archive/old_versions/"
    echo "  ✓ 已移动 v3.1/"
fi

echo ""
echo "[3/4] 移动旧的启动脚本..."
if [ -f "run_reservation.bat" ]; then
    mv "run_reservation.bat" "_archive/old_versions/"
    echo "  ✓ 已移动 run_reservation.bat"
fi
if [ -f "启动Web管理.bat" ]; then
    mv "启动Web管理.bat" "_archive/old_versions/"
    echo "  ✓ 已移动 启动Web管理.bat"
fi

echo ""
echo "[4/4] 移动旧文档..."
if [ -f "README.md" ] && [ -f "README_V2.md" ]; then
    mv "README.md" "_archive/old_versions/README_old.md"
    echo "  ✓ 已移动 README.md"
fi
if [ -f "ID说明书.md" ]; then
    mv "ID说明书.md" "_archive/old_versions/"
    echo "  ✓ 已移动 ID说明书.md"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "✅ 清理完成！"
echo ""
echo "📁 旧文件已移动到: _archive/old_versions/"
echo ""
echo "当前项目结构:"
echo "  ✓ backend/ - 后端服务"
echo "  ✓ frontend/ - 原生前端"
echo "  ✓ frontend-vue/ - Vue3 前端"
echo "  ✓ 新版本启动脚本"
echo "  ✓ 新版本文档"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
