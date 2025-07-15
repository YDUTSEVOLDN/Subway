import os
from app import create_app

# 获取配置环境
config_name = os.getenv('FLASK_ENV', 'development')
app = create_app(config_name)

if __name__ == '__main__':
    host = os.getenv('API_HOST', '127.0.0.1')
    port = int(os.getenv('API_PORT', 5000))
    debug = os.getenv('FLASK_DEBUG', '1') == '1'

    print("🚀 启动Metro Prediction API")
    print(f"🌐 地址: http://{host}:{port}")
    print(f"🔧 调试模式: {debug}")
    print("📚 API接口:")
    print(f"   GET  /                                    - 服务信息")
    print(f"   GET  /health                              - 健康检查")
    print(f"   GET  /db-test                             - 数据库连接测试")
    print(f"   GET  /api/v1/metro/stations               - 获取站点列表")
    print(f"   GET  /api/v1/metro/data/summary           - 获取数据概览")
    print(f"   GET  /api/v1/metro/data/historical-3days  - 获取历史3天数据")
    print(f"   GET  /api/v1/metro/data/raw               - 获取原始数据")
    print("-" * 60)

    app.run(host=host, port=port, debug=debug)