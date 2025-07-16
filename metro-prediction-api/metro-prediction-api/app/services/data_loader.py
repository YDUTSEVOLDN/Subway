from ..models.metro_data import MetroHistoricalData
from ..extensions import db

class DataLoader:
    @staticmethod
    def load_recent_data(days=3, station=None):
        """确保在调用时已有应用上下文"""
        from datetime import datetime, timedelta
        
        if not hasattr(db, 'session'):
            raise RuntimeError("数据库未初始化")
            
        cutoff_date = datetime.now() - timedelta(days=days)
        query = db.session.query(MetroHistoricalData).filter(
            MetroHistoricalData.date >= cutoff_date.date()
        ).order_by(
            MetroHistoricalData.date.asc(),
            getattr(MetroHistoricalData, 'time_slot', MetroHistoricalData.time_slot).asc()
        )
        
        if station:
            query = query.filter(MetroHistoricalData.station == station)
            
        return query.all()