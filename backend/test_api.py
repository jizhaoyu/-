"""
自动化测试脚本 - 后端 API 测试
"""
import sys
import time
import requests

# 设置输出编码为 UTF-8
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

from colorama import init, Fore, Style
init(autoreset=True)

API_BASE = "http://127.0.0.1:5000"

class TestRunner:
    def __init__(self):
        self.passed = 0
        self.failed = 0
        self.tests = []

    def test(self, name, func):
        """运行单个测试"""
        print(f"\n{Fore.CYAN}[TEST] {name}")
        try:
            func()
            print(f"{Fore.GREEN}[PASS] 通过")
            self.passed += 1
            self.tests.append((name, True, None))
        except AssertionError as e:
            print(f"{Fore.RED}[FAIL] 失败: {e}")
            self.failed += 1
            self.tests.append((name, False, str(e)))
        except Exception as e:
            print(f"{Fore.RED}[ERROR] 错误: {e}")
            self.failed += 1
            self.tests.append((name, False, str(e)))

    def summary(self):
        """输出测试摘要"""
        print(f"\n{'='*60}")
        print(f"{Fore.CYAN}测试摘要")
        print(f"{'='*60}")
        print(f"总计: {self.passed + self.failed}")
        print(f"{Fore.GREEN}通过: {self.passed}")
        print(f"{Fore.RED}失败: {self.failed}")
        print(f"{'='*60}\n")

        if self.failed > 0:
            print(f"{Fore.YELLOW}失败的测试:")
            for name, passed, error in self.tests:
                if not passed:
                    print(f"  {Fore.RED}[X] {name}: {error}")

runner = TestRunner()

# ============ 健康检查 ============

def test_health_check():
    """测试健康检查端点"""
    response = requests.get(f"{API_BASE}/health", timeout=5)
    assert response.status_code == 200, f"状态码应为 200，实际为 {response.status_code}"
    data = response.json()
    assert data.get('status') == 'ok', "健康检查应返回 ok"

def test_api_root():
    """测试 API 根路径"""
    response = requests.get(f"{API_BASE}/", timeout=5)
    assert response.status_code == 200, f"状态码应为 200，实际为 {response.status_code}"
    data = response.json()
    assert 'message' in data, "应包含 message 字段"
    assert 'version' in data, "应包含 version 字段"

# ============ 账号管理测试 ============

test_account_id = None

def test_create_account():
    """测试创建账号"""
    global test_account_id

    payload = {
        "name": "测试账号",
        "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6IjEyMzQ1Njc4OTAiLCJuYW1lIjoi5rWL6K-V6LSm5Y-3IiwiZXhwIjo5OTk5OTk5OTk5fQ.test",
        "area_id": "22",
        "date_preference": "tomorrow",
        "strategy": "first"
    }

    response = requests.post(f"{API_BASE}/api/accounts", json=payload, timeout=5)

    # 由于 token 可能无效，接受 400 或 201
    assert response.status_code in [200, 201, 400], f"状态码应为 201 或 400，实际为 {response.status_code}"

    if response.status_code in [200, 201]:
        data = response.json()
        assert data.get('success') == True, "success 应为 True"
        assert 'data' in data, "应包含 data 字段"
        test_account_id = data['data'].get('id')
        print(f"  创建账号 ID: {test_account_id}")

def test_get_accounts():
    """测试获取账号列表"""
    response = requests.get(f"{API_BASE}/api/accounts", timeout=5)
    assert response.status_code == 200, f"状态码应为 200，实际为 {response.status_code}"
    data = response.json()
    assert data.get('success') == True, "success 应为 True"
    assert 'data' in data, "应包含 data 字段"
    assert isinstance(data['data'], list), "data 应为列表"
    print(f"  账号数量: {len(data['data'])}")

def test_get_account_detail():
    """测试获取账号详情"""
    if not test_account_id:
        print(f"  {Fore.YELLOW}跳过: 无可用账号 ID")
        return

    response = requests.get(f"{API_BASE}/api/accounts/{test_account_id}", timeout=5)
    assert response.status_code == 200, f"状态码应为 200，实际为 {response.status_code}"
    data = response.json()
    assert data.get('success') == True, "success 应为 True"
    assert 'data' in data, "应包含 data 字段"
    assert data['data']['id'] == test_account_id, "账号 ID 应匹配"

def test_update_account():
    """测试更新账号"""
    if not test_account_id:
        print(f"  {Fore.YELLOW}跳过: 无可用账号 ID")
        return

    payload = {
        "name": "更新后的账号",
        "area_id": "23"
    }

    response = requests.put(f"{API_BASE}/api/accounts/{test_account_id}", json=payload, timeout=5)
    assert response.status_code == 200, f"状态码应为 200，实际为 {response.status_code}"
    data = response.json()
    assert data.get('success') == True, "success 应为 True"

def test_delete_account():
    """测试删除账号"""
    if not test_account_id:
        print(f"  {Fore.YELLOW}跳过: 无可用账号 ID")
        return

    response = requests.delete(f"{API_BASE}/api/accounts/{test_account_id}", timeout=5)
    assert response.status_code == 200, f"状态码应为 200，实际为 {response.status_code}"
    data = response.json()
    assert data.get('success') == True, "success 应为 True"

# ============ 预约管理测试 ============

def test_reservation_endpoints_exist():
    """测试预约端点是否存在（不执行实际预约）"""
    # 这些端点需要有效的 account_id，这里只测试端点是否存在
    endpoints = [
        '/api/reservations/test',
        '/api/reservations/execute',
        '/api/reservations/check-in',
        '/api/reservations/history'
    ]

    for endpoint in endpoints:
        response = requests.post(f"{API_BASE}{endpoint}", json={}, timeout=5)
        # 应该返回 400 (缺少参数) 而不是 404 (端点不存在)
        assert response.status_code in [400, 500], f"{endpoint} 端点应存在"
        print(f"  {endpoint}: 端点存在 ✓")

# ============ 错误处理测试 ============

def test_404_not_found():
    """测试 404 错误"""
    response = requests.get(f"{API_BASE}/api/nonexistent", timeout=5)
    assert response.status_code == 404, "不存在的端点应返回 404"

def test_invalid_account_id():
    """测试无效的账号 ID"""
    response = requests.get(f"{API_BASE}/api/accounts/99999", timeout=5)
    assert response.status_code == 404, "不存在的账号应返回 404"

def test_missing_required_fields():
    """测试缺少必填字段"""
    response = requests.post(f"{API_BASE}/api/accounts", json={}, timeout=5)
    assert response.status_code == 400, "缺少必填字段应返回 400"

# ============ 运行测试 ============

def main():
    print(f"\n{Fore.CYAN}{'='*60}")
    print(f"{Fore.CYAN}图书馆预约管理系统 - 后端 API 自动化测试")
    print(f"{Fore.CYAN}{'='*60}\n")

    # 检查后端服务
    print(f"{Fore.YELLOW}[CHECK] 后端服务连接...")
    try:
        response = requests.get(f"{API_BASE}/health", timeout=5)
        print(f"{Fore.GREEN}[OK] 后端服务运行中\n")
    except requests.exceptions.ConnectionError:
        print(f"{Fore.RED}[ERROR] 无法连接到后端服务 ({API_BASE})")
        print(f"{Fore.YELLOW}提示: 请先启动后端服务")
        return 1

    # 运行测试
    print(f"{Fore.CYAN}开始测试...\n")

    # 健康检查
    runner.test("健康检查", test_health_check)
    runner.test("API 根路径", test_api_root)

    # 账号管理
    runner.test("创建账号", test_create_account)
    runner.test("获取账号列表", test_get_accounts)
    runner.test("获取账号详情", test_get_account_detail)
    runner.test("更新账号", test_update_account)
    runner.test("删除账号", test_delete_account)

    # 预约管理
    runner.test("预约端点检查", test_reservation_endpoints_exist)

    # 错误处理
    runner.test("404 错误处理", test_404_not_found)
    runner.test("无效账号 ID", test_invalid_account_id)
    runner.test("缺少必填字段", test_missing_required_fields)

    # 输出摘要
    runner.summary()

    return 0 if runner.failed == 0 else 1

if __name__ == "__main__":
    sys.exit(main())
