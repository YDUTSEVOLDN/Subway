from datetime import date, time, datetime
from sqlalchemy import Column, Integer, String, Date, Time, DateTime, Numeric
from app.database.connection import Base

class MetroHistoricalData(Base):
    __tablename__ = 'metro_historical_data'

    id = Column(Integer, primary_key=True, autoincrement=True)
    date = Column(Date, nullable=False, index=True)
    station = Column(String(100), nullable=False, index=True)
    district = Column(String(50), nullable=False)
    time_slot = Column(Time, nullable=False)
    in_count = Column(Integer, nullable=False)
    out_count = Column(Integer, nullable=False)
    time_only = Column(Time)
    is_transfer = Column(Integer, nullable=False)
    temperature = Column(Numeric(10, 6))
    humidity = Column(Numeric(10, 6))
    wind_speed = Column(Numeric(10, 6))
    created_at = Column(DateTime(6))
    data_source = Column(String(50))
    updated_at = Column(DateTime(6))