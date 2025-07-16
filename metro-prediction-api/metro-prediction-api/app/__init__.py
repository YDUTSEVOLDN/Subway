
import os
import sys
from flask import Flask, jsonify
from flask_sqlalchemy import SQLAlchemy
from flask_cors import CORS

# 添加路径配置
current_dir = os.path.dirname(os.path.abspath(__file__))
parent_dir = os.path.dirname(current_dir)

if current_dir not in sys.path:
    sys.path.insert(0, current_dir)
if parent_dir not in sys.path:
    sys.path.insert(0, parent_dir)

# 初始化扩展
db = SQLAlchemy()
cors = CORS()

def create_app(config_name='default'):
    """应用工厂函数"""
    app = Flask(__name__)

    
    # 加载配置
    try:
        # 延迟导入配置
        from config import config
        app.config.from_object(config[config_name])
    except (ImportError, KeyError) as e:
        print(f"配置加载失败: {e}")
        # # 使用默认配置
        # app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///metro.db'
        # app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
        # app.config['SECRET_KEY'] = 'dev-secret-key'
    
        # 初始化扩展
    db.init_app(app)
    cors.init_app(app)
    
    # # 在应用上下文中导入模型（避免循环导入）
    # with app.app_context():
    #     try:
    #         from models.metro_data import MetroHistoricalData
    #     except ImportError as e:
    #         print(f"导入模型失败: {e}")
    
    # # 延迟导入和初始化模型
    # try:
    #     from models.ONNXModelAdapter import ONNXModelAdapter
    #     from services.prediction_service import prediction_service
        
    #     # 获取模型文件路径
    #     models_dir = os.path.join(current_dir, 'models')
    #     in_model_path = os.path.join(models_dir, 'in_count_model.onnx')
    #     out_model_path = os.path.join(models_dir, 'out_count_model.onnx')
        
    #     # 验证模型文件是否存在
    #     if os.path.exists(in_model_path) and os.path.exists(out_model_path):
    #         in_model = ONNXModelAdapter(in_model_path)
    #         out_model = ONNXModelAdapter(out_model_path)
    #         app.prediction_service = prediction_service(in_model, out_model)
    #     else:
    #         print(f"警告: 模型文件不存在")
    #         print(f"进站模型: {in_model_path} - 存在: {os.path.exists(in_model_path)}")
    #         print(f"出站模型: {out_model_path} - 存在: {os.path.exists(out_model_path)}")
    #         app.prediction_service = None
            
    # except Exception as e:
    #     print(f"模型初始化失败: {e}")
    #     app.prediction_service = None
    
    # # 注册蓝图
    # try:
    #     from api import bp as api_bp
    #     app.register_blueprint(api_bp, url_prefix='/api/v1')
    # except ImportError as e:
    #     print(f"API蓝图注册失败: {e}")
    
    # # 基础路由
    # @app.route('/')
    # def index():
    #     return jsonify({
    #         'message': 'Metro Prediction API',
    #         'version': '1.0.0',
    #         'status': 'running'
    #     })
    
    # @app.route('/health')
    # def health():
    #     return jsonify({'status': 'healthy'})
    
    @app.route('/db-test')
    def test_db():
        try:
            with db.engine.connect() as connection:
                result = connection.execute(db.text('SELECT 1'))
                
            return jsonify({
                'status': 'success',
                'message': '数据库连接正常',
                'database': app.config['SQLALCHEMY_DATABASE_URI'].split('/')[-1]
            })
        except Exception as e:
            return jsonify({
                'status': 'error',
                'message': '数据库连接失败',
                'error': str(e)
            }), 500
    
    # @app.route('/create-tables')
    # def create_tables():
    #     try:
    #         with app.app_context():
    #             db.create_all()
    #             inspector = db.inspect(db.engine)
    #             tables = inspector.get_table_names()
                
    #             return jsonify({
    #                 'status': 'success',
    #                 'message': '数据库表创建成功',
    #                 'tables': tables,
    #                 'metro_table_exists': 'metro_historical_data' in tables
    #             })
    #     except Exception as e:
    #         return jsonify({
    #             'status': 'error',
    #             'message': '创建数据库表失败',
    #             'error': str(e)
    #         }), 500
    
    # @app.route('/debug-paths')
    # def debug_paths():
    #     return jsonify({
    #         'current_dir': current_dir,
    #         'models_dir': os.path.join(current_dir, 'models'),
    #         'models_dir_exists': os.path.exists(os.path.join(current_dir, 'models')),
    #         'in_model_exists': os.path.exists(os.path.join(current_dir, 'models', 'in_count_model.onnx')),
    #         'out_model_exists': os.path.exists(os.path.join(current_dir, 'models', 'out_count_model.onnx')),
    #         'prediction_service_loaded': hasattr(app, 'prediction_service') and app.prediction_service is not None
    #     })
    
    return app

# ================================================================
# 2. 创建 extensions.py 文件 - 避免循环导入
# ================================================================

# app/extensions.py
from flask_sqlalchemy import SQLAlchemy
from flask_cors import CORS

db = SQLAlchemy()
cors = CORS()