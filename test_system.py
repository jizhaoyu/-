"""
自动化测试脚本 - 完整系统测试
"""
import os
import sys
import time
import subprocess
import requests
from pathlib import Path

# 设置输出编码为 UTF-8
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

from colorama import init, Fore, Style
init(autoreset=True)

class SystemTester:
    def __init__(self):
        self.base_dir = Path(__file__).parent
        self.backend_dir = self.base_dir / "backend"
        self.frontend_dir = self.base_dir / "frontend"
        self.frontend_vue_dir = self.base_dir / "frontend-vue"

        self.results = {
            'structure': [],
            'backend': [],
            'frontend': [],
            'integration': []
        }

    def print_header(self, title):
        """打印测试标题"""
        print(f"\n{Fore.CYAN}{'='*60}")
        print(f"{Fore.CYAN}{title}")
        print(f"{Fore.CYAN}{'='*60}\n")

    def test_result(self, category, name, passed, message=""):
        """记录测试结果"""
        status = f"{Fore.GREEN}[PASS]" if passed else f"{Fore.RED}[FAIL]"
        print(f"{status} {name}")
        if message:
            print(f"  {Fore.YELLOW}{message}")
        self.results[category].append((name, passed, message))

    # ============ 项目结构测试 ============

    def test_project_structure(self):
        """测试项目结构完整性"""
        self.print_header("1. 项目结构测试")

        # 后端文件
        backend_files = [
            "backend/app.py",
            "backend/requirements.txt",
            "backend/database/db.py",
            "backend/models/account.py",
            "backend/routes/account_routes.py",
            "backend/routes/reservation_routes.py",
            "backend/utils/token_utils.py",
            "backend/utils/library_api.py",
        ]

        for file_path in backend_files:
            full_path = self.base_dir / file_path
            self.test_result(
                'structure',
                f"后端文件: {file_path}",
                full_path.exists()
            )

        # 原生前端文件
        frontend_files = [
            "frontend/index.html",
            "frontend/css/style.css",
            "frontend/js/app.js",
        ]

        for file_path in frontend_files:
            full_path = self.base_dir / file_path
            self.test_result(
                'structure',
                f"原生前端: {file_path}",
                full_path.exists()
            )

        # Vue3 前端文件
        vue_files = [
            "frontend-vue/package.json",
            "frontend-vue/vite.config.ts",
            "frontend-vue/src/main.ts",
            "frontend-vue/src/App.vue",
            "frontend-vue/src/views/HomeView.vue",
            "frontend-vue/src/components/AccountForm.vue",
            "frontend-vue/src/api/index.ts",
            "frontend-vue/src/stores/account.ts",
        ]

        for file_path in vue_files:
            full_path = self.base_dir / file_path
            self.test_result(
                'structure',
                f"Vue3 前端: {file_path}",
                full_path.exists()
            )

        # 启动脚本
        scripts = [
            "一键启动.bat",
            "启动后端服务.bat",
            "启动前端界面.bat",
            "启动Vue3前端.bat",
        ]

        for script in scripts:
            full_path = self.base_dir / script
            self.test_result(
                'structure',
                f"启动脚本: {script}",
                full_path.exists()
            )

        # 文档文件
        docs = [
            "README_V2.md",
            "快速启动指南.md",
            "项目结构说明.md",
            "API文档.md",
            "项目总览.md",
        ]

        for doc in docs:
            full_path = self.base_dir / doc
            self.test_result(
                'structure',
                f"文档: {doc}",
                full_path.exists()
            )

    # ============ 后端测试 ============

    def test_backend_imports(self):
        """测试后端模块导入"""
        self.print_header("2. 后端模块测试")

        try:
            sys.path.insert(0, str(self.backend_dir))

            # 测试导入
            try:
                from database.db import db, init_db
                self.test_result('backend', "导入 database.db", True)
            except Exception as e:
                self.test_result('backend', "导入 database.db", False, str(e))

            try:
                from models.account import Account, Reservation
                self.test_result('backend', "导入 models.account", True)
            except Exception as e:
                self.test_result('backend', "导入 models.account", False, str(e))

            try:
                from utils.token_utils import normalize_token, parse_jwt
                self.test_result('backend', "导入 utils.token_utils", True)
            except Exception as e:
                self.test_result('backend', "导入 utils.token_utils", False, str(e))

            try:
                from utils.library_api import get_date, encrypt_data
                self.test_result('backend', "导入 utils.library_api", True)
            except Exception as e:
                self.test_result('backend', "导入 utils.library_api", False, str(e))

        except Exception as e:
            self.test_result('backend', "模块导入", False, str(e))

    def test_backend_service(self):
        """测试后端服务"""
        self.print_header("3. 后端服务测试")

        try:
            response = requests.get("http://127.0.0.1:5000/health", timeout=3)
            self.test_result(
                'backend',
                "后端服务连接",
                response.status_code == 200,
                f"状态码: {response.status_code}"
            )
        except requests.exceptions.ConnectionError:
            self.test_result(
                'backend',
                "后端服务连接",
                False,
                "无法连接（服务可能未启动）"
            )
        except Exception as e:
            self.test_result('backend', "后端服务连接", False, str(e))

    # ============ 前端测试 ============

    def test_frontend_files(self):
        """测试前端文件完整性"""
        self.print_header("4. 前端文件测试")

        # 检查原生前端 HTML
        html_file = self.frontend_dir / "index.html"
        if html_file.exists():
            content = html_file.read_text(encoding='utf-8')
            self.test_result(
                'frontend',
                "HTML 包含 GSAP",
                "gsap" in content.lower()
            )
            self.test_result(
                'frontend',
                "HTML 包含 app.js",
                "app.js" in content
            )
        else:
            self.test_result('frontend', "HTML 文件存在", False)

        # 检查 CSS
        css_file = self.frontend_dir / "css" / "style.css"
        if css_file.exists():
            content = css_file.read_text(encoding='utf-8')
            self.test_result(
                'frontend',
                "CSS 包含渐变样式",
                "gradient" in content.lower()
            )
        else:
            self.test_result('frontend', "CSS 文件存在", False)

        # 检查 JavaScript
        js_file = self.frontend_dir / "js" / "app.js"
        if js_file.exists():
            content = js_file.read_text(encoding='utf-8')
            self.test_result(
                'frontend',
                "JS 包含 API 调用",
                "fetch" in content or "axios" in content
            )
            self.test_result(
                'frontend',
                "JS 包含 GSAP 动画",
                "gsap" in content.lower()
            )
        else:
            self.test_result('frontend', "JS 文件存在", False)

    def test_vue_project(self):
        """测试 Vue3 项目配置"""
        self.print_header("5. Vue3 项目测试")

        # 检查 package.json
        package_json = self.frontend_vue_dir / "package.json"
        if package_json.exists():
            import json
            try:
                data = json.loads(package_json.read_text(encoding='utf-8'))
                deps = data.get('dependencies', {})

                self.test_result(
                    'frontend',
                    "Vue3 依赖",
                    'vue' in deps
                )
                self.test_result(
                    'frontend',
                    "Arco Design 依赖",
                    '@arco-design/web-vue' in deps
                )
                self.test_result(
                    'frontend',
                    "Pinia 依赖",
                    'pinia' in deps
                )
                self.test_result(
                    'frontend',
                    "GSAP 依赖",
                    'gsap' in deps
                )
            except Exception as e:
                self.test_result('frontend', "package.json 解析", False, str(e))
        else:
            self.test_result('frontend', "package.json 存在", False)

        # 检查 TypeScript 配置
        tsconfig = self.frontend_vue_dir / "tsconfig.json"
        self.test_result(
            'frontend',
            "TypeScript 配置",
            tsconfig.exists()
        )

        # 检查 Vite 配置
        vite_config = self.frontend_vue_dir / "vite.config.ts"
        self.test_result(
            'frontend',
            "Vite 配置",
            vite_config.exists()
        )

    # ============ 集成测试 ============

    def test_integration(self):
        """测试集成功能"""
        self.print_header("6. 集成测试")

        # 测试 API 端点
        try:
            response = requests.get("http://127.0.0.1:5000/api/accounts", timeout=3)
            self.test_result(
                'integration',
                "API: 获取账号列表",
                response.status_code == 200
            )

            if response.status_code == 200:
                data = response.json()
                self.test_result(
                    'integration',
                    "API: 返回格式正确",
                    'success' in data and 'data' in data
                )
        except Exception as e:
            self.test_result('integration', "API 测试", False, str(e))

    # ============ 主测试流程 ============

    def run_all_tests(self):
        """运行所有测试"""
        print(f"\n{Fore.CYAN}{'='*60}")
        print(f"{Fore.CYAN}图书馆预约管理系统 - 自动化测试")
        print(f"{Fore.CYAN}{'='*60}\n")

        self.test_project_structure()
        self.test_backend_imports()
        self.test_backend_service()
        self.test_frontend_files()
        self.test_vue_project()
        self.test_integration()

        # 输出总结
        self.print_summary()

    def print_summary(self):
        """输出测试总结"""
        self.print_header("测试总结")

        total_passed = 0
        total_failed = 0

        for category, tests in self.results.items():
            passed = sum(1 for _, p, _ in tests if p)
            failed = sum(1 for _, p, _ in tests if not p)
            total_passed += passed
            total_failed += failed

            print(f"{Fore.CYAN}{category.upper()}:")
            print(f"  {Fore.GREEN}通过: {passed}")
            print(f"  {Fore.RED}失败: {failed}")

        print(f"\n{Fore.CYAN}总计:")
        print(f"  {Fore.GREEN}通过: {total_passed}")
        print(f"  {Fore.RED}失败: {total_failed}")
        print(f"  总数: {total_passed + total_failed}")

        if total_failed > 0:
            print(f"\n{Fore.YELLOW}失败的测试:")
            for category, tests in self.results.items():
                for name, passed, message in tests:
                    if not passed:
                        print(f"  {Fore.RED}[X] [{category}] {name}")
                        if message:
                            print(f"    {Fore.YELLOW}{message}")

        print(f"\n{Fore.CYAN}{'='*60}\n")

        return 0 if total_failed == 0 else 1

def main():
    tester = SystemTester()
    return tester.run_all_tests()

if __name__ == "__main__":
    sys.exit(main())
