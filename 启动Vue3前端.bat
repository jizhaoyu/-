@echo off
chcp 65001 >nul
echo.
echo ╔════════════════════════════════════════════════════════╗
echo ║     图书馆预约管理系统 v2.0 - Vue3 前端界面          ║
echo ╚════════════════════════════════════════════════════════╝
echo.

cd /d "%~dp0frontend-vue"

if not exist "node_modules" (
    echo [1/2] 安装依赖...
    call npm install
    if errorlevel 1 (
        echo.
        echo 依赖安装失败，尝试使用国内镜像...
        call npm install --registry=https://registry.npmmirror.com
    )
)

echo.
echo [2/2] 启动开发服务器...
echo.
echo ====================================
echo   Vue3 前端: http://127.0.0.1:8080
echo   API 代理: http://127.0.0.1:5000
echo ====================================
echo.

call npm run dev

pause
