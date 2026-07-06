@echo off
chcp 65001 >nul
cls
echo ============================================================
echo   Library Reservation System v2.0 - Start All Services
echo ============================================================
echo.
echo [INFO] Backend and Vue3 frontend will start in new windows.
echo.

set "ROOT=%~dp0"

:: ==================== Backend ====================
echo [1/2] Starting backend service...
start "Backend Service (Flask :5000)" /d "%ROOT%backend" cmd /k "if not exist .venv python -m venv .venv & call .venv\Scripts\activate.bat & pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple & python app.py"

:: Wait for backend to be ready
timeout /t 5 >nul

:: ==================== Vue3 Frontend ====================
echo [2/2] Starting Vue3 frontend...
start "Vue3 Frontend (Vite :8080)" /d "%ROOT%frontend-vue" cmd /k "if not exist node_modules npm install & npm run dev"

echo.
echo ============================================================
echo   Services launched in separate windows.
echo.
echo   Backend : http://127.0.0.1:5000
echo   Frontend: http://127.0.0.1:8080
echo   Proxy   : /api  ^-^>  http://127.0.0.1:5000
echo ============================================================
echo.
echo [INFO] Wait a moment, then open http://127.0.0.1:8080
echo [INFO] Close the corresponding window to stop a service.
echo.

timeout /t 8 >nul
start http://127.0.0.1:8080

echo Press any key to close this window (services keep running)...
pause >nul
