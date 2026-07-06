"""
账号数据模型
"""
from datetime import datetime
from database.db import db


class Account(db.Model):
    """图书馆账号模型"""
    __tablename__ = 'accounts'

    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    name = db.Column(db.String(50), nullable=False, comment='姓名')
    token = db.Column(db.Text, nullable=False, comment='认证Token')
    area_id = db.Column(db.String(20), default='22', comment='区域ID')
    date_preference = db.Column(db.String(20), default='tomorrow', comment='预约日期偏好: today/tomorrow')
    strategy = db.Column(db.String(20), default='first', comment='座位选择策略: first/random')
    created_at = db.Column(db.DateTime, default=datetime.now, comment='创建时间')
    updated_at = db.Column(db.DateTime, default=datetime.now, onupdate=datetime.now, comment='更新时间')

    # 关联预约记录
    reservations = db.relationship('Reservation', backref='account', lazy='dynamic', cascade='all, delete-orphan')

    def to_dict(self):
        """转换为字典"""
        return {
            'id': self.id,
            'name': self.name,
            'token': self.token,
            'area_id': self.area_id,
            'date_preference': self.date_preference,
            'strategy': self.strategy,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'updated_at': self.updated_at.isoformat() if self.updated_at else None
        }

    def __repr__(self):
        return f'<Account {self.name}>'


class Reservation(db.Model):
    """预约记录模型"""
    __tablename__ = 'reservations'

    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    account_id = db.Column(db.Integer, db.ForeignKey('accounts.id'), nullable=False, comment='账号ID')
    reservation_id = db.Column(db.String(50), comment='服务器预约ID')
    seat_no = db.Column(db.String(50), comment='座位号')
    seat_id = db.Column(db.String(50), comment='座位ID')
    area_name = db.Column(db.String(100), comment='区域名称')
    segment_id = db.Column(db.String(50), comment='时间段ID')
    reserve_date = db.Column(db.String(20), comment='预约日期')
    start_time = db.Column(db.String(20), comment='开始时间')
    end_time = db.Column(db.String(20), comment='结束时间')
    status = db.Column(db.String(20), default='pending', comment='状态: pending/confirmed/cancelled')
    message = db.Column(db.Text, comment='预约响应消息')
    created_at = db.Column(db.DateTime, default=datetime.now, comment='创建时间')

    def to_dict(self):
        """转换为字典"""
        return {
            'id': self.id,
            'account_id': self.account_id,
            'reservation_id': self.reservation_id,
            'seat_no': self.seat_no,
            'seat_id': self.seat_id,
            'area_name': self.area_name,
            'segment_id': self.segment_id,
            'reserve_date': self.reserve_date,
            'start_time': self.start_time,
            'end_time': self.end_time,
            'status': self.status,
            'message': self.message,
            'created_at': self.created_at.isoformat() if self.created_at else None
        }

    def __repr__(self):
        return f'<Reservation {self.reservation_id}>'
