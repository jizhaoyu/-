"""
Token 工具函数
"""
import base64
import json
import time


def normalize_token(raw_token):
    """标准化 token，移除 bearer 前缀和引号"""
    if not raw_token:
        return None

    token = raw_token.strip().strip('"').strip("'")

    # 移除 bearer 前缀
    lower = token.lower()
    if lower.startswith('bearer '):
        token = token.split(' ', 1)[1].strip()
    elif lower.startswith('bearer'):
        token = token[6:].strip()

    return token


def parse_jwt(token):
    """解析 JWT token 获取 payload"""
    try:
        parts = token.split('.')
        if len(parts) != 3:
            return None

        # 解码 payload（第二部分）
        payload_b64 = parts[1]
        # Base64 URL 解码需要补齐
        payload_b64 += '=' * (-len(payload_b64) % 4)
        payload_bytes = base64.urlsafe_b64decode(payload_b64)
        payload = json.loads(payload_bytes)

        return payload
    except Exception as e:
        print(f"解析 JWT 失败: {e}")
        return None


def validate_token(token):
    """验证 token 是否过期"""
    payload = parse_jwt(token)
    if not payload:
        return False, "Token 格式无效"

    exp = payload.get('exp')
    if not exp:
        return True, "无法获取过期时间，假定有效"

    now = int(time.time())
    if exp <= now:
        expired_time = time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(exp))
        return False, f"Token 已过期（{expired_time}）"

    remaining_minutes = (exp - now) // 60
    return True, f"有效，剩余 {remaining_minutes} 分钟"


def get_token_info(token):
    """获取 token 详细信息"""
    payload = parse_jwt(token)
    if not payload:
        return None

    exp = payload.get('exp')
    now = int(time.time())

    return {
        'payload': payload,
        'exp': exp,
        'exp_formatted': time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(exp)) if exp else None,
        'is_expired': exp <= now if exp else None,
        'remaining_seconds': exp - now if exp else None,
        'remaining_minutes': (exp - now) // 60 if exp else None,
        'user_name': payload.get('name'),
        'user_id': payload.get('id')
    }
