@echo off
chcp 65001 >nul
echo.
echo ====================================
echo   图书馆预约管理系统 - 前端界面
echo ====================================
echo.

cd /d "%~dp0frontend"

echo 正在启动前端服务...
echo.
echo 前端地址: http://127.0.0.1:8080
echo 请确保后端服务已启动（运行 启动后端服务.bat）
echo.
echo ====================================
echo.

python -m http.server 8080

pause
