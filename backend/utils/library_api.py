"""
图书馆 API 封装
"""
import json
import requests
from datetime import datetime
import base64
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import padding


BASE_URL = "http://libyy.qfnu.edu.cn"
TIMEOUT = 20

DEFAULT_HEADERS = {
    "Content-Type": "application/json",
    "Connection": "keep-alive",
    "Accept": "application/json, text/plain, */*",
    "lang": "zh",
    "X-Requested-With": "XMLHttpRequest",
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36",
    "Origin": BASE_URL,
    "Referer": f"{BASE_URL}/h5/index.html",
}


def get_headers(token):
    """构造请求头"""
    headers = dict(DEFAULT_HEADERS)
    headers["Authorization"] = f"Bearer {token}"
    return headers


def get_body_auth(token):
    """构造请求体中的 authorization 字段"""
    return f"bearer{token}"


def encrypt_data(data_str):
    """AES 加密（用于确认预约）"""
    aes_key = datetime.now().strftime("%Y%m%d")
    aes_key = aes_key + aes_key[::-1]  # 日期 + 反转日期
    aes_iv = "ZZWBKJ_ZHIHUAWEI"

    cipher = Cipher(
        algorithms.AES(aes_key.encode('utf-8')),
        modes.CBC(aes_iv.encode('utf-8')),
        backend=default_backend()
    )
    encryptor = cipher.encryptor()
    padder = padding.PKCS7(algorithms.AES.block_size).padder()
    padded_data = padder.update(data_str.encode('utf-8')) + padder.finalize()
    ciphertext = encryptor.update(padded_data) + encryptor.finalize()

    return base64.b64encode(ciphertext).decode()


def post_api(endpoint, token, payload):
    """统一的 POST 请求封装"""
    url = f"{BASE_URL}{endpoint}"
    body = dict(payload)
    body['authorization'] = get_body_auth(token)

    try:
        response = requests.post(
            url,
            json=body,
            headers=get_headers(token),
            timeout=TIMEOUT
        )
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        return {'code': -1, 'msg': f'请求失败: {str(e)}'}
    except ValueError:
        return {'code': -1, 'msg': '响应格式错误'}


def get_date_segments(token, area_id):
    """获取可预约日期和时间段"""
    return post_api('/api/Seat/date', token, {'build_id': str(area_id)})


def get_seats(token, area_id, segment_id, day, start_time, end_time):
    """获取座位列表"""
    payload = {
        'area': str(area_id),
        'segment': str(segment_id),
        'day': day,
        'startTime': start_time,
        'endTime': end_time
    }
    return post_api('/api/Seat/seat', token, payload)


def confirm_reservation(token, seat_id, segment_id):
    """确认预约"""
    origin_data = json.dumps(
        {'seat_id': str(seat_id), 'segment': str(segment_id)},
        ensure_ascii=False,
        separators=(',', ':')
    )
    payload = {'aesjson': encrypt_data(origin_data)}
    return post_api('/api/Seat/confirm', token, payload)


def get_current_reservations(token):
    """获取当前预约记录"""
    return post_api('/api/index/subscribe', token, {})


def cancel_reservation(token, reservation_id):
    """取消预约"""
    return post_api('/api/Space/cancel', token, {'id': str(reservation_id)})


def check_in(token):
    """签到"""
    json_data = '{"method":"checkin"}'
    payload = {'aesjson': encrypt_data(json_data)}
    return post_api('/api/Seat/touch_qr_books', token, payload)


def sign_out(token):
    """签退"""
    return post_api('/api/Space/checkout', token, {})


def get_date(date_type='tomorrow'):
    """获取日期字符串"""
    from datetime import datetime, timedelta

    if date_type == 'today':
        return datetime.now().strftime('%Y-%m-%d')
    elif date_type == 'tomorrow':
        return (datetime.now() + timedelta(days=1)).strftime('%Y-%m-%d')
    else:
        return date_type  # 已经是 YYYY-MM-DD 格式
