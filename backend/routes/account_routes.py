"""
账号管理路由
"""
from flask import Blueprint, request, jsonify
from database.db import db
from models.account import Account
from utils.token_utils import normalize_token, validate_token, get_token_info

account_bp = Blueprint('account', __name__, url_prefix='/api/accounts')


@account_bp.route('', methods=['GET'])
def get_accounts():
    """获取所有账号"""
    try:
        accounts = Account.query.order_by(Account.created_at.desc()).all()
        result = []

        for account in accounts:
            account_dict = account.to_dict()

            # 添加 token 状态信息
            is_valid, message = validate_token(account.token)
            token_info = get_token_info(account.token)

            account_dict['token_status'] = {
                'is_valid': is_valid,
                'message': message,
                'info': token_info
            }

            result.append(account_dict)

        return jsonify({
            'success': True,
            'data': result
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'获取账号列表失败: {str(e)}'
        }), 500


@account_bp.route('/<int:account_id>', methods=['GET'])
def get_account(account_id):
    """获取单个账号详情"""
    try:
        account = Account.query.get(account_id)
        if not account:
            return jsonify({
                'success': False,
                'message': '账号不存在'
            }), 404

        account_dict = account.to_dict()

        # 添加 token 状态信息
        is_valid, message = validate_token(account.token)
        token_info = get_token_info(account.token)

        account_dict['token_status'] = {
            'is_valid': is_valid,
            'message': message,
            'info': token_info
        }

        return jsonify({
            'success': True,
            'data': account_dict
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'获取账号详情失败: {str(e)}'
        }), 500


@account_bp.route('', methods=['POST'])
def create_account():
    """创建账号"""
    try:
        data = request.get_json()

        # 验证必填字段
        if not data.get('name') or not data.get('token'):
            return jsonify({
                'success': False,
                'message': '姓名和 Token 为必填项'
            }), 400

        # 标准化 token
        normalized_token = normalize_token(data['token'])
        if not normalized_token:
            return jsonify({
                'success': False,
                'message': 'Token 格式无效'
            }), 400

        # 验证 token
        is_valid, message = validate_token(normalized_token)
        if not is_valid:
            return jsonify({
                'success': False,
                'message': f'Token 无效: {message}'
            }), 400

        # 创建账号
        account = Account(
            name=data['name'],
            token=normalized_token,
            area_id=data.get('area_id', '22'),
            date_preference=data.get('date_preference', 'tomorrow'),
            strategy=data.get('strategy', 'first')
        )

        db.session.add(account)
        db.session.commit()

        return jsonify({
            'success': True,
            'message': '账号添加成功',
            'data': account.to_dict()
        }), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({
            'success': False,
            'message': f'添加账号失败: {str(e)}'
        }), 500


@account_bp.route('/<int:account_id>', methods=['PUT'])
def update_account(account_id):
    """更新账号"""
    try:
        account = Account.query.get(account_id)
        if not account:
            return jsonify({
                'success': False,
                'message': '账号不存在'
            }), 404

        data = request.get_json()

        # 更新字段
        if 'name' in data:
            account.name = data['name']

        if 'token' in data:
            normalized_token = normalize_token(data['token'])
            if not normalized_token:
                return jsonify({
                    'success': False,
                    'message': 'Token 格式无效'
                }), 400

            is_valid, message = validate_token(normalized_token)
            if not is_valid:
                return jsonify({
                    'success': False,
                    'message': f'Token 无效: {message}'
                }), 400

            account.token = normalized_token

        if 'area_id' in data:
            account.area_id = data['area_id']

        if 'date_preference' in data:
            account.date_preference = data['date_preference']

        if 'strategy' in data:
            account.strategy = data['strategy']

        db.session.commit()

        return jsonify({
            'success': True,
            'message': '账号更新成功',
            'data': account.to_dict()
        })
    except Exception as e:
        db.session.rollback()
        return jsonify({
            'success': False,
            'message': f'更新账号失败: {str(e)}'
        }), 500


@account_bp.route('/<int:account_id>', methods=['DELETE'])
def delete_account(account_id):
    """删除账号"""
    try:
        account = Account.query.get(account_id)
        if not account:
            return jsonify({
                'success': False,
                'message': '账号不存在'
            }), 404

        db.session.delete(account)
        db.session.commit()

        return jsonify({
            'success': True,
            'message': '账号删除成功'
        })
    except Exception as e:
        db.session.rollback()
        return jsonify({
            'success': False,
            'message': f'删除账号失败: {str(e)}'
        }), 500
