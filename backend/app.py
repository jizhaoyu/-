"""
Flask 后端主程序
"""
import os
from flask import Flask, jsonify
from flask_cors import CORS
from database.db import init_db
from routes.account_routes import account_bp
from routes.reservation_routes import reservation_bp


def create_app():
    """创建 Flask 应用"""
    app = Flask(__name__)

    # 配置
    basedir = os.path.abspath(os.path.dirname(__file__))
    app.config['SQLALCHEMY_DATABASE_URI'] = f'sqlite:///{os.path.join(basedir, "library.db")}'
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
    app.config['JSON_AS_ASCII'] = False  # 支持中文

    # 初始化 CORS
    CORS(app, resources={
        r"/api/*": {
            "origins": "*",
            "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
            "allow_headers": ["Content-Type", "Authorization"]
        }
    })

    # 初始化数据库
    init_db(app)

    # 注册蓝图
    app.register_blueprint(account_bp)
    app.register_blueprint(reservation_bp)

    # 根路由
    @app.route('/')
    def index():
        return jsonify({
            'message': '图书馆预约管理系统 API',
            'version': '2.0',
            'endpoints': {
                'accounts': '/api/accounts',
                'reservations': '/api/reservations'
            }
        })

    # 健康检查
    @app.route('/health')
    def health():
        return jsonify({'status': 'ok'})

    return app


if __name__ == '__main__':
    app = create_app()
    print("\n" + "="*60)
    print("📚 图书馆预约管理系统后端服务")
    print("="*60)
    print("✓ 服务地址: http://127.0.0.1:5000")
    print("✓ API 文档: http://127.0.0.1:5000/")
    print("✓ 数据库: SQLite (library.db)")
    print("="*60 + "\n")

    app.run(
        host='127.0.0.1',
        port=5000,
        debug=True,
        use_reloader=True
    )
