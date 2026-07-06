@echo off
chcp 65001 >nul
echo.
echo ====================================
echo   图书馆预约管理系统 - 后端服务
echo ====================================
echo.

cd /d "%~dp0backend"

if not exist ".venv" (
    echo [1/3] 创建虚拟环境...
    python -m venv .venv
    if errorlevel 1 (
        echo 创建虚拟环境失败，请确保已安装 Python 3.7+
        pause
        exit /b 1
    )
)

echo [2/3] 激活虚拟环境...
call .venv\Scripts\activate.bat

echo [3/3] 安装依赖...
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

echo.
echo ====================================
echo   启动后端服务...
echo ====================================
echo.

python app.py

pause
