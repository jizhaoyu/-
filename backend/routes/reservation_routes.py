"""
预约管理路由
"""
from flask import Blueprint, request, jsonify
from database.db import db
from models.account import Account, Reservation
from utils.library_api import (
    get_date_segments, get_seats, confirm_reservation,
    get_current_reservations, cancel_reservation,
    check_in, sign_out, get_date
)
import random

reservation_bp = Blueprint('reservation', __name__, url_prefix='/api/reservations')


@reservation_bp.route('/test', methods=['POST'])
def test_reserve():
    """测试预约（不实际提交）"""
    try:
        data = request.get_json()
        account_id = data.get('account_id')

        if not account_id:
            return jsonify({
                'success': False,
                'message': '缺少 account_id 参数'
            }), 400

        account = Account.query.get(account_id)
        if not account:
            return jsonify({
                'success': False,
                'message': '账号不存在'
            }), 404

        # 获取日期
        reserve_date = get_date(account.date_preference)

        # 获取可预约时间段
        segments_result = get_date_segments(account.token, account.area_id)
        if segments_result.get('code') != 1:
            return jsonify({
                'success': False,
                'message': f"获取时间段失败: {segments_result.get('msg')}"
            }), 400

        # 找到对应日期的时间段
        date_data = segments_result.get('data', [])
        day_info = next((item for item in date_data if item.get('day') == reserve_date), None)

        if not day_info:
            return jsonify({
                'success': False,
                'message': f'没有找到 {reserve_date} 的可预约时间段'
            }), 400

        available_segments = [s for s in day_info.get('times', []) if str(s.get('status')) == '1']
        if not available_segments:
            return jsonify({
                'success': False,
                'message': f'{reserve_date} 没有可预约时间段'
            }), 400

        # 选择第一个可用时间段
        segment = available_segments[0]

        # 获取座位列表
        seats_result = get_seats(
            account.token,
            account.area_id,
            segment['id'],
            reserve_date,
            segment.get('start'),
            segment.get('end')
        )

        if seats_result.get('code') != 1:
            return jsonify({
                'success': False,
                'message': f"获取座位失败: {seats_result.get('msg')}"
            }), 400

        # 筛选可用座位
        all_seats = seats_result.get('data', [])
        available_seats = [
            seat for seat in all_seats
            if str(seat.get('status')) == '1' and str(seat.get('in_label', '1')) != '0'
        ]

        if not available_seats:
            return jsonify({
                'success': False,
                'message': '当前没有可用座位'
            }), 400

        # 根据策略选择座位
        if account.strategy == 'random':
            selected_seat = random.choice(available_seats)
        else:  # first
            selected_seat = available_seats[0]

        return jsonify({
            'success': True,
            'message': '测试成功',
            'data': {
                'date': reserve_date,
                'segment': {
                    'id': segment['id'],
                    'start': segment.get('start'),
                    'end': segment.get('end')
                },
                'seat': {
                    'id': selected_seat.get('id'),
                    'no': selected_seat.get('no') or selected_seat.get('name'),
                    'name': selected_seat.get('name')
                },
                'available_seats_count': len(available_seats),
                'total_seats_count': len(all_seats)
            }
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'测试失败: {str(e)}'
        }), 500


@reservation_bp.route('/execute', methods=['POST'])
def execute_reserve():
    """执行预约"""
    try:
        data = request.get_json()
        account_id = data.get('account_id')

        if not account_id:
            return jsonify({
                'success': False,
                'message': '缺少 account_id 参数'
            }), 400

        account = Account.query.get(account_id)
        if not account:
            return jsonify({
                'success': False,
                'message': '账号不存在'
            }), 404

        # 获取日期
        reserve_date = get_date(account.date_preference)

        # 获取可预约时间段
        segments_result = get_date_segments(account.token, account.area_id)
        if segments_result.get('code') != 1:
            return jsonify({
                'success': False,
                'message': f"获取时间段失败: {segments_result.get('msg')}"
            }), 400

        date_data = segments_result.get('data', [])
        day_info = next((item for item in date_data if item.get('day') == reserve_date), None)

        if not day_info:
            return jsonify({
                'success': False,
                'message': f'没有找到 {reserve_date} 的可预约时间段'
            }), 400

        available_segments = [s for s in day_info.get('times', []) if str(s.get('status')) == '1']
        if not available_segments:
            return jsonify({
                'success': False,
                'message': f'{reserve_date} 没有可预约时间段'
            }), 400

        segment = available_segments[0]

        # 获取座位列表
        seats_result = get_seats(
            account.token,
            account.area_id,
            segment['id'],
            reserve_date,
            segment.get('start'),
            segment.get('end')
        )

        if seats_result.get('code') != 1:
            return jsonify({
                'success': False,
                'message': f"获取座位失败: {seats_result.get('msg')}"
            }), 400

        all_seats = seats_result.get('data', [])
        available_seats = [
            seat for seat in all_seats
            if str(seat.get('status')) == '1' and str(seat.get('in_label', '1')) != '0'
        ]

        if not available_seats:
            return jsonify({
                'success': False,
                'message': '当前没有可用座位'
            }), 400

        # 根据策略选择座位
        if account.strategy == 'random':
            selected_seat = random.choice(available_seats)
        else:
            selected_seat = available_seats[0]

        # 提交预约
        reserve_result = confirm_reservation(
            account.token,
            selected_seat.get('id'),
            segment['id']
        )

        # 保存预约记录
        reservation = Reservation(
            account_id=account.id,
            reservation_id=str(reserve_result.get('data', {}).get('id', '')),
            seat_no=selected_seat.get('no') or selected_seat.get('name'),
            seat_id=str(selected_seat.get('id')),
            area_name=f"区域 {account.area_id}",
            segment_id=str(segment['id']),
            reserve_date=reserve_date,
            start_time=segment.get('start'),
            end_time=segment.get('end'),
            status='confirmed' if reserve_result.get('code') == 1 else 'failed',
            message=reserve_result.get('msg', '')
        )

        db.session.add(reservation)
        db.session.commit()

        if reserve_result.get('code') == 1:
            return jsonify({
                'success': True,
                'message': '预约成功',
                'data': {
                    'reservation': reservation.to_dict(),
                    'api_response': reserve_result
                }
            })
        else:
            return jsonify({
                'success': False,
                'message': f"预约失败: {reserve_result.get('msg')}",
                'data': {
                    'reservation': reservation.to_dict(),
                    'api_response': reserve_result
                }
            }), 400

    except Exception as e:
        db.session.rollback()
        return jsonify({
            'success': False,
            'message': f'预约失败: {str(e)}'
        }), 500


@reservation_bp.route('/current', methods=['POST'])
def get_current():
    """获取当前预约"""
    try:
        data = request.get_json()
        account_id = data.get('account_id')

        if not account_id:
            return jsonify({
                'success': False,
                'message': '缺少 account_id 参数'
            }), 400

        account = Account.query.get(account_id)
        if not account:
            return jsonify({
                'success': False,
                'message': '账号不存在'
            }), 404

        result = get_current_reservations(account.token)

        if result.get('code') == 1:
            return jsonify({
                'success': True,
                'data': result.get('data', [])
            })
        else:
            return jsonify({
                'success': False,
                'message': f"获取失败: {result.get('msg')}"
            }), 400

    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'获取失败: {str(e)}'
        }), 500


@reservation_bp.route('/cancel', methods=['POST'])
def cancel_reserve():
    """取消预约"""
    try:
        data = request.get_json()
        account_id = data.get('account_id')
        reservation_id = data.get('reservation_id')

        if not account_id:
            return jsonify({
                'success': False,
                'message': '缺少 account_id 参数'
            }), 400

        account = Account.query.get(account_id)
        if not account:
            return jsonify({
                'success': False,
                'message': '账号不存在'
            }), 404

        result = cancel_reservation(account.token, reservation_id)

        if result.get('code') == 1:
            # 更新本地记录状态
            local_reservation = Reservation.query.filter_by(
                account_id=account.id,
                reservation_id=reservation_id
            ).first()

            if local_reservation:
                local_reservation.status = 'cancelled'
                db.session.commit()

            return jsonify({
                'success': True,
                'message': '取消成功',
                'data': result
            })
        else:
            return jsonify({
                'success': False,
                'message': f"取消失败: {result.get('msg')}",
                'data': result
            }), 400

    except Exception as e:
        db.session.rollback()
        return jsonify({
            'success': False,
            'message': f'取消失败: {str(e)}'
        }), 500


@reservation_bp.route('/check-in', methods=['POST'])
def do_check_in():
    """签到"""
    try:
        data = request.get_json()
        account_id = data.get('account_id')

        if not account_id:
            return jsonify({
                'success': False,
                'message': '缺少 account_id 参数'
            }), 400

        account = Account.query.get(account_id)
        if not account:
            return jsonify({
                'success': False,
                'message': '账号不存在'
            }), 404

        result = check_in(account.token)

        if result.get('code') == 1 or result.get('msg') in ['签到成功', '使用中,不用重复签到！']:
            return jsonify({
                'success': True,
                'message': result.get('msg', '签到成功'),
                'data': result
            })
        else:
            return jsonify({
                'success': False,
                'message': f"签到失败: {result.get('msg')}",
                'data': result
            }), 400

    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'签到失败: {str(e)}'
        }), 500


@reservation_bp.route('/sign-out', methods=['POST'])
def do_sign_out():
    """签退"""
    try:
        data = request.get_json()
        account_id = data.get('account_id')

        if not account_id:
            return jsonify({
                'success': False,
                'message': '缺少 account_id 参数'
            }), 400

        account = Account.query.get(account_id)
        if not account:
            return jsonify({
                'success': False,
                'message': '账号不存在'
            }), 404

        result = sign_out(account.token)

        if result.get('code') == 1:
            return jsonify({
                'success': True,
                'message': result.get('msg', '签退成功'),
                'data': result
            })
        else:
            return jsonify({
                'success': False,
                'message': f"签退失败: {result.get('msg')}",
                'data': result
            }), 400

    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'签退失败: {str(e)}'
        }), 500


@reservation_bp.route('/history', methods=['GET'])
def get_history():
    """获取历史预约记录"""
    try:
        account_id = request.args.get('account_id')

        if account_id:
            reservations = Reservation.query.filter_by(
                account_id=int(account_id)
            ).order_by(Reservation.created_at.desc()).all()
        else:
            reservations = Reservation.query.order_by(
                Reservation.created_at.desc()
            ).limit(50).all()

        return jsonify({
            'success': True,
            'data': [r.to_dict() for r in reservations]
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'获取历史记录失败: {str(e)}'
        }), 500
