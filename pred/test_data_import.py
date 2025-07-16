from app.services.data_loader import DataLoader
from app.services.prediction import MetroPredictor
from datetime import date
def test_predictions():
    """修改点5：添加分阶段测试验证"""
    print("=== 测试阶段1：数据加载 ===")
    data_loader = DataLoader()
    
    # 新增：检查第一批数据
    first_batch = next(data_loader.load_last_n_days_data(days=6))
    print("[TEST] 第一批数据第一条记录:", first_batch[0])
    
    # 新增：验证数据字段
    REQUIRED_KEYS = {'date', 'time_slot', 'station', 'temperature', 'humidity', 'is_transfer'}
    first_record_keys = set(first_batch[0].keys())
    if not REQUIRED_KEYS.issubset(first_record_keys):
        missing = REQUIRED_KEYS - first_record_keys
        raise AssertionError(f"数据缺少必要字段: {missing}")
    
    print("=== 测试阶段2：预测执行 ===")
    predictor = MetroPredictor()
    
    # 新增：先用小批量数据测试
    test_sample = first_batch[:100]  # 只取100条测试
    print(f"[TEST] 使用{len(test_sample)}条数据进行预测...")
    
    try:
        results = predictor.predict(test_sample)
        print("[TEST] 预测执行成功!")
    except Exception as e:
        print("[TEST] 预测失败:", str(e))
        raise
    
    print("=== 测试阶段3：结果验证 ===")
    # 新增：验证预测结果结构
    first_result = results[0]
    print("[TEST] 第一条预测结果:", first_result)
    
    REQUIRED_RESULT_KEYS = {'date', 'time_slot', 'station', 'predicted_in_count', 'predicted_out_count'}
    if not all(k in first_result for k in REQUIRED_RESULT_KEYS):
        missing = REQUIRED_RESULT_KEYS - set(first_result.keys())
        raise AssertionError(f"预测结果缺少字段: {missing}")
    
    print("[SUCCESS] 所有测试通过!")

    
def test_metro_data_import():
    # 测试加载最近7天的数据
    end_date = date.today()
    start_date = date(end_date.year, end_date.month, end_date.day - 1)
    
    print(f"Loading metro data from {start_date} to {end_date}")
    
    total_records = 0
    for batch in DataLoader.load_metro_data(start_date=start_date, end_date=end_date):
        print(f"Loaded batch with {len(batch)} records")
        total_records += len(batch)
        
        # 这里可以添加数据处理逻辑
        for record in batch:
            # 示例: 打印第一条记录的摘要
            if total_records == 1:
                print(f"Sample record: {record.date} {record.station} "
                      f"{record.time_slot} - In: {record.in_count}, Out: {record.out_count}")
    
    print(f"Total records loaded: {total_records}")

if __name__ == "__main__":
    test_predictions()