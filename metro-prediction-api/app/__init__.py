from flask import Flask, jsonify
from flask_sqlalchemy import SQLAlchemy
from flask_cors import CORS
from config import config
import logging

# 初始化扩展
db = SQLAlchemy()
cors = CORS()

def create_app(config_name='default'):
    """应用工厂函数"""
    app = Flask(__name__)
    
    # 加载配置
    app.config.from_object(config[config_name])
    
    # 初始化扩展
    db.init_app(app)
    cors.init_app(app)
    
    # 导入模型（确保在db.create_all()之前导入）
    from app.models.metro_data import MetroHistoricalData
    
    # 注册蓝图
    from app.api import bp as api_bp
    app.register_blueprint(api_bp, url_prefix='/api/v1')
    
    # 基础路由
    @app.route('/')
    def index():
        return jsonify({
            'message': 'Metro Prediction API',
            'version': '1.0.0',
            'status': 'running'
        })
    
    @app.route('/health')
    def health():
        return jsonify({'status': 'healthy'})
    
    # 数据库连接测试
    @app.route('/db-test')
    def test_db():
        try:
            # 测试数据库连接
            with db.engine.connect() as connection:
                result = connection.execute(db.text('SELECT 1'))
                
            return jsonify({
                'status': 'success',
                'message': '数据库连接正常',
                'database': app.config['SQLALCHEMY_DATABASE_URI'].split('@')[1].split('/')[1]
            })
        except Exception as e:
            return jsonify({
                'status': 'error',
                'message': '数据库连接失败',
                'error': str(e)
            }), 500
    
    # 创建数据库表的路由
    @app.route('/create-tables')
    def create_tables():
        try:
            with app.app_context():
                # 创建所有表
                db.create_all()
                
                # 验证表是否创建成功
                inspector = db.inspect(db.engine)
                tables = inspector.get_table_names()
                
                return jsonify({
                    'status': 'success',
                    'message': '数据库表创建成功',
                    'tables': tables,
                    'metro_table_exists': 'metro_historical_data' in tables
                })
        except Exception as e:
            return jsonify({
                'status': 'error',
                'message': '创建数据库表失败',
                'error': str(e)
            }), 500
    
    # 查看表结构的路由
    @app.route('/table-structure')
    def table_structure():
        try:
            inspector = db.inspect(db.engine)
            
            if 'metro_historical_data' not in inspector.get_table_names():
                return jsonify({
                    'status': 'error',
                    'message': '表 metro_historical_data 不存在'
                }), 404
            
            # 获取表结构
            columns = inspector.get_columns('metro_historical_data')
            indexes = inspector.get_indexes('metro_historical_data')
            
            column_info = []
            for col in columns:
                column_info.append({
                    'name': col['name'],
                    'type': str(col['type']),
                    'nullable': col['nullable'],
                    'default': str(col['default']) if col['default'] else None,
                    'primary_key': col.get('primary_key', False)
                })
            
            return jsonify({
                'status': 'success',
                'table_name': 'metro_historical_data',
                'columns': column_info,
                'indexes': [idx['name'] for idx in indexes],
                'column_count': len(columns)
            })
            
        except Exception as e:
            return jsonify({
                'status': 'error',
                'message': '获取表结构失败',
                'error': str(e)
            }), 500
    
    return app