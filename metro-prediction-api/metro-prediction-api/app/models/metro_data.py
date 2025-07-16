from extensions import db
from sqlalchemy import Column, Integer, String, Float, Date, Time, DateTime
from datetime import datetime


   
class MetroHistoricalData(db.Model):
    __tablename__ = 'metro_historical_data'  # 确保与表名一致
    
    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    date = db.Column(db.Date, nullable=False, index=True)
    station = db.Column(db.String(100), nullable=False, index=True)
    district = db.Column(db.String(50), nullable=False)
    time_slot = db.Column(db.Time, nullable=False)
    in_count = db.Column(db.Integer, nullable=False)
    out_count = db.Column(db.Integer, nullable=False)
    time_only = db.Column(db.Time)
    is_transfer = db.Column(db.Integer, nullable=False)
    temperature = db.Column(db.Numeric(10, 6))
    humidity = db.Column(db.Numeric(10, 6))
    wind_speed = db.Column(db.Numeric(10, 6))
    created_at = db.Column(db.DateTime(6))
    data_source = db.Column(db.String(50))
    updated_at = db.Column(db.DateTime(6))

    def __repr__(self):
        return f'<Station {self.station} on {self.date} at {self.time_slot}>'

    # def to_dict(self):
    #     return {
    #         'id': self.id,
    #         'date': self.date.isoformat() if self.date else None,
    #         'station': self.station,
    #         'district': self.district,
    #         'time': self.time.isoformat() if self.time else None,
    #         'in_count': self.in_count,
    #         'out_count': self.out_count,
    #         'created_at': self.created_at.isoformat() if self.created_at else None,
    #         'updated_at': self.updated_at.isoformat() if self.updated_at else None
    #     }
