import os
from dotenv import load_dotenv

load_dotenv()

class Config:
    # MySQL 配置
    # 修改后的 MySQL 配置
    SQLALCHEMY_DATABASE_URI = os.getenv(
        'DATABASE_URL',
        'mysql+pymysql://admin:password@39.96.195.232/subbike'
    )
    
    # 将连接参数移到 ENGINE_OPTIONS
    SQLALCHEMY_ENGINE_OPTIONS = {
        'pool_recycle': 3600,
        'pool_pre_ping': True,
        'connect_args': {
            'allow_public_key_retrieval': True,
            'charset': 'utf8',
            'use_affected_rows': True
        }
    }
    SQLALCHEMY_TRACK_MODIFICATIONS = False