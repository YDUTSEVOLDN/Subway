from app import db
from datetime import datetime
from sqlalchemy import Index

class MetroHistoricalData(db.Model):
    """地铁历史数据模型 - 完全对应原数据库表结构"""
    __tablename__ = 'metro_prediction_data'

    # 定义表索引
    __table_args__ = (
        Index('idx_date_station', 'date', 'station'),
        Index('idx_station_time', 'station', 'time_slot'),
        Index('idx_date_time', 'date', 'time_slot'),
    )

    # 字段定义 - 完全对应数据库表
    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)

    # 基础信息字段
    date = db.Column(db.Date, nullable=False, comment='日期')
    station = db.Column(db.String(100), nullable=False, comment='站点名称')
    district = db.Column(db.String(50), nullable=False, comment='区')
    time_slot = db.Column(db.Time, nullable=False, comment='时间段')

    # 流量数据字段
    in_count = db.Column(db.Integer, nullable=False, comment='预测进站人数')
    out_count = db.Column(db.Integer, nullable=False, comment='预测出站人数')


    def to_dict(self):
        """转换为字典格式，便于JSON序列化"""
        return {
            'id': self.id,
            'date': self.date.isoformat() if self.date else None,
            'station': self.station,
            'district': self.district,
            'time_slot': str(self.time_slot) if self.time_slot else None,
            'in_count': self.in_count,
            'out_count': self.out_count
        }

    def __repr__(self):
        return f'<MetroHistoricalData {self.station} {self.date} {self.time_slot}>'