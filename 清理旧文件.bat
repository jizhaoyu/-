@echo off
chcp 65001 >nul
cls
echo.
echo ╔════════════════════════════════════════════════════════╗
echo ║         项目清理工具 - 移除旧版本文件               ║
echo ╚════════════════════════════════════════════════════════╝
echo.
echo [警告] 此脚本将移动以下旧版本文件到 _archive 目录:
echo   - py/ 目录 (旧版本 Python 脚本)
echo   - web/ 目录 (旧版本 Web 文件)
echo   - v3.1/ 目录 (历史版本)
echo   - 旧的 .bat 脚本
echo   - 旧的 .md 文档
echo.
echo 保留的文件:
echo   - backend/ (新后端)
echo   - frontend/ (新原生前端)
echo   - frontend-vue/ (新Vue3前端)
echo   - 所有新的启动脚本和文档
echo.
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

set /p confirm="确认清理旧文件? (输入 YES 继续): "
if not "%confirm%"=="YES" (
    echo.
    echo 已取消清理操作
    pause
    exit /b 0
)

echo.
echo [1/4] 创建归档目录...
if not exist "_archive" mkdir "_archive"
if not exist "_archive\old_versions" mkdir "_archive\old_versions"

echo.
echo [2/4] 移动旧版本目录...
if exist "py" (
    move "py" "_archive\old_versions\py" >nul 2>&1
    echo   ✓ 已移动 py/
)
if exist "web" (
    move "web" "_archive\old_versions\web" >nul 2>&1
    echo   ✓ 已移动 web/
)
if exist "v3.1" (
    move "v3.1" "_archive\old_versions\v3.1" >nul 2>&1
    echo   ✓ 已移动 v3.1/
)

echo.
echo [3/4] 移动旧的启动脚本...
if exist "run_reservation.bat" (
    move "run_reservation.bat" "_archive\old_versions\" >nul 2>&1
    echo   ✓ 已移动 run_reservation.bat
)
if exist "启动Web管理.bat" (
    move "启动Web管理.bat" "_archive\old_versions\" >nul 2>&1
    echo   ✓ 已移动 启动Web管理.bat
)

echo.
echo [4/4] 移动旧文档...
if exist "README.md" (
    if not exist "README_V2.md" (
        echo   ! 跳过 README.md (未找到新版本)
    ) else (
        move "README.md" "_archive\old_versions\README_old.md" >nul 2>&1
        echo   ✓ 已移动 README.md
    )
)
if exist "ID说明书.md" (
    move "ID说明书.md" "_archive\old_versions\" >nul 2>&1
    echo   ✓ 已移动 ID说明书.md
)

echo.
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.
echo ✅ 清理完成！
echo.
echo 📁 旧文件已移动到: _archive\old_versions\
echo.
echo 当前项目结构:
echo   ✓ backend/ - 后端服务
echo   ✓ frontend/ - 原生前端
echo   ✓ frontend-vue/ - Vue3 前端
echo   ✓ 新版本启动脚本
echo   ✓ 新版本文档
echo.
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.
pause
