# """
# 数据库初始化脚本
# """
# import sys
# import os

# # 添加项目根目录到Python路径
# sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# from app import create_app, db
# from app.models.metro_data import MetroHistoricalData

# def init_database():
#     """初始化数据库"""
#     app = create_app('development')
    
#     with app.app_context():
#         print("开始创建数据库表...")
        
#         try:
#             # 删除所有表（如果需要重新创建）
#             # db.drop_all()
#             # print("已删除所有表")
            
#             # 创建所有表
#             db.create_all()
#             print("✅ 数据库表创建成功")
            
#             # 验证表结构
#             inspector = db.inspect(db.engine)
#             tables = inspector.get_table_names()
#             print(f"📋 已创建的表: {tables}")
            
#             if 'metro_historical_data' in tables:
#                 columns = inspector.get_columns('metro_historical_data')
#                 print(f"📊 metro_historical_data 表字段数: {len(columns)}")
                
#                 for col in columns:
#                     print(f"   - {col['name']}: {col['type']} ({'NOT NULL' if not col['nullable'] else 'NULL'})")
            
#             print("🎉 数据库初始化完成")
            
#         except Exception as e:
#             print(f"❌ 数据库初始化失败: {str(e)}")
#             return False
    
#     return True

# if __name__ == '__main__':
#     init_database()