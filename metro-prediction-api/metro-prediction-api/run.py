from tabulate import tabulate
from app import create_app
from app.services.data_loader import DataLoader

app = create_app()

if __name__ == '__main__':
    # 确保在应用上下文中执行数据库操作
    with app.app_context():
        try:
            data = DataLoader.load_recent_data(days=6)
            
            def format_record(r):
                return [
                    r.id,
                    r.date.strftime('%Y-%m-%d'),
                    r.station,
                    getattr(r, 'time_slot', r.time).strftime('%H:%M'),  # 兼容不同字段名
                    r.in_count,
                    r.out_count,
                    f"{r.temperature}°C" if hasattr(r, 'temperature') else 'N/A'
                ]

            print("\n=== 前10行数据 ===")
            print(tabulate(
                [format_record(r) for r in data[:10]],
                headers=['ID', 'Date', 'Station', 'Time', 'In', 'Out', 'Temp'],
                tablefmt='grid'
            ))
            
            print("\n=== 最后10行数据 ===")
            print(tabulate(
                [format_record(r) for r in data[-10:]],
                headers=['ID', 'Date', 'Station', 'Time', 'In', 'Out', 'Temp'],
                tablefmt='grid'
            ))
            
            print(f"\n总共加载了 {len(data)} 条记录")
            
        except Exception as e:
            print(f"数据加载失败: {str(e)}")
            raise

    app.run(host='0.0.0.0', port=5000, debug=True, use_reloader=False)