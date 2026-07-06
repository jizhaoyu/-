@echo off
chcp 65001 >nul
cls
echo.
echo ╔════════════════════════════════════════════════════════╗
echo ║         图书馆预约管理系统 - 自动化测试             ║
echo ╚════════════════════════════════════════════════════════╝
echo.
echo [提示] 本脚本将运行完整的自动化测试套件
echo.
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

:: 检查 Python
python --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到 Python，请先安装 Python 3.7+
    pause
    exit /b 1
)

:: 检查并安装依赖
echo [1/3] 检查测试依赖...
pip show colorama >nul 2>&1
if errorlevel 1 (
    echo   安装 colorama...
    pip install colorama -q
)

pip show requests >nul 2>&1
if errorlevel 1 (
    echo   安装 requests...
    pip install requests -q
)

echo.
echo [2/3] 运行系统测试...
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
python test_system.py
set SYSTEM_TEST_RESULT=%ERRORLEVEL%

echo.
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

:: 检查后端是否运行
echo [3/3] 检查后端服务状态...
curl -s http://127.0.0.1:5000/health >nul 2>&1
if errorlevel 1 (
    echo   [警告] 后端服务未运行，跳过 API 测试
    echo   [提示] 启动后端服务后可运行: python backend\test_api.py
    goto :summary
)

echo   后端服务运行中 ✓
echo.
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.
echo [API测试] 运行后端 API 测试...
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
cd backend
python test_api.py
set API_TEST_RESULT=%ERRORLEVEL%
cd ..

:summary
echo.
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.
echo ✅ 测试完成
echo.
if %SYSTEM_TEST_RESULT% == 0 (
    echo   系统测试: 通过 ✓
) else (
    echo   系统测试: 失败 ✗
)

if defined API_TEST_RESULT (
    if %API_TEST_RESULT% == 0 (
        echo   API 测试: 通过 ✓
    ) else (
        echo   API 测试: 失败 ✗
    )
) else (
    echo   API 测试: 未运行 -
)

echo.
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

pause
