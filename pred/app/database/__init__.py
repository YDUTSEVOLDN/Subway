
from .connection import db_connection, Base
from .models import MetroHistoricalData
# 确保所有模型被加载
def init_db():
    Base.metadata.create_all(bind=db_connection.engine)