"""
Flask 后端主程序
"""
import os
from urllib.parse import quote_plus
from dotenv import load_dotenv
from flask import Flask, jsonify
from flask_cors import CORS
from database.db import init_db
from routes.account_routes import account_bp
from routes.reservation_routes import reservation_bp

# 加载 backend/.env 中的数据库配置
basedir = os.path.abspath(os.path.dirname(__file__))
load_dotenv(os.path.join(basedir, '.env'))


def build_database_uri():
    """根据 .env 构建数据库连接串（默认 MySQL，可回退 SQLite）"""
    # DB_TYPE=mysql（默认）或 sqlite
    db_type = os.getenv('DB_TYPE', 'mysql').lower()

    if db_type == 'sqlite':
        return f'sqlite:///{os.path.join(basedir, "library.db")}'

    # MySQL 连接配置（密码/用户名做 URL 转义，兼容特殊字符）
    host = os.getenv('DB_HOST', '127.0.0.1')
    port = os.getenv('DB_PORT', '3306')
    user = os.getenv('DB_USER', 'root')
    password = quote_plus(os.getenv('DB_PASSWORD', ''))
    name = os.getenv('DB_NAME', 'library_reservation')
    charset = os.getenv('DB_CHARSET', 'utf8mb4')
    return f'mysql+pymysql://{user}:{password}@{host}:{port}/{name}?charset={charset}'


def create_app():
    """创建 Flask 应用"""
    app = Flask(__name__)

    # 配置
    app.config['SQLALCHEMY_DATABASE_URI'] = build_database_uri()
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
    # 连接池：MySQL 长时间空闲会断开，pool_pre_ping/pool_recycle 保活
    app.config['SQLALCHEMY_ENGINE_OPTIONS'] = {
        'pool_pre_ping': True,
        'pool_recycle': 3600,
    }
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
    db_uri = app.config['SQLALCHEMY_DATABASE_URI']
    db_kind = 'SQLite' if db_uri.startswith('sqlite') else 'MySQL'
    print("\n" + "="*60)
    print("图书馆预约管理系统后端服务")
    print("="*60)
    print("服务地址: http://127.0.0.1:5000")
    print("API 文档: http://127.0.0.1:5000/")
    print(f"数据库类型: {db_kind}")
    print("="*60 + "\n")

    app.run(
        host='127.0.0.1',
        port=5000,
        debug=True,
        use_reloader=True
    )
