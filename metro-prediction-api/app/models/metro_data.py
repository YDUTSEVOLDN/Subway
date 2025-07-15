from app import db
from datetime import datetime
from sqlalchemy import Index

class MetroHistoricalData(db.Model):
    """地铁历史数据模型 - 完全对应原数据库表结构"""
    __tablename__ = 'metro_historical_data'
    
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
    district = db.Column(db.String(50), nullable=False, comment='区域')
    time_slot = db.Column(db.Time, nullable=False, comment='时间段')
    
    # 流量数据字段
    in_count = db.Column(db.Integer, nullable=False, comment='进站人数')
    out_count = db.Column(db.Integer, nullable=False, comment='出站人数')
    time_only = db.Column(db.Time, comment='仅时间')
    
    # 属性字段
    is_transfer = db.Column(db.Integer, nullable=False, comment='是否换乘站(0/1)')
    
    # 天气数据字段
    temperature = db.Column(db.Numeric(10, 6), comment='温度')
    humidity = db.Column(db.Numeric(10, 6), comment='湿度')
    wind_speed = db.Column(db.Numeric(10, 6), comment='风速')
    

    
    def to_dict(self):
        """转换为字典格式，便于JSON序列化"""
        return {
            'id': self.id,
            'date': self.date.isoformat() if self.date else None,
            'station': self.station,
            'district': self.district,
            'time_slot': str(self.time_slot) if self.time_slot else None,
            'in_count': self.in_count,
            'out_count': self.out_count,
            'time_only': str(self.time_only) if self.time_only else None,
            'is_transfer': self.is_transfer,
            'temperature': float(self.temperature) if self.temperature else None,
            'humidity': float(self.humidity) if self.humidity else None,
            'wind_speed': float(self.wind_speed) if self.wind_speed else None,

        }
    
    def __repr__(self):
        return f'<MetroHistoricalData {self.station} {self.date} {self.time_slot}>'