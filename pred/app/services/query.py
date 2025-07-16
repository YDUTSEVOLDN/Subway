def get_prediction_data(session, batch_size):
    """
    从数据库获取预测用数据（示例）
    实际实现应根据你的数据库结构和需求调整
    """
    # 示例：假设你有一个 PredictionData 模型
    offset = 0
    while True:
        batch = session.query(PredictionData).offset(offset).limit(batch_size).all()
        if not batch:
            break
        yield batch
        offset += batch_size