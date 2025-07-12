<template>
  <div class="path-test-container">
    <h2>路径规划服务测试</h2>
    
    <el-card class="test-card">
      <template #header>
        <span>测试配置</span>
      </template>
      
      <el-form :model="testConfig" label-width="120px">
        <el-form-item label="起点站">
          <el-select v-model="testConfig.sourceId" placeholder="选择起点站">
            <el-option
              v-for="station in stations"
              :key="station.id"
              :label="station.name"
              :value="station.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="终点站">
          <el-select v-model="testConfig.targetId" placeholder="选择终点站">
            <el-option
              v-for="station in stations"
              :key="station.id"
              :label="station.name"
              :value="station.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="出行方式">
          <el-radio-group v-model="testConfig.vehicleType">
            <el-radio-button label="bike">骑行</el-radio-button>
            <el-radio-button label="car">驾车</el-radio-button>
            <el-radio-button label="walk">步行</el-radio-button>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="优化目标">
          <el-radio-group v-model="testConfig.optimizeFor">
            <el-radio-button label="distance">最短距离</el-radio-button>
            <el-radio-button label="time">最短时间</el-radio-button>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="testPathPlanning" :loading="testing">
            开始测试
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <el-card v-if="testResult" class="result-card">
      <template #header>
        <span>测试结果</span>
      </template>
      
      <el-descriptions :column="2" border>
        <el-descriptions-item label="距离">{{ testResult.distance.toFixed(2) }} 公里</el-descriptions-item>
        <el-descriptions-item label="时间">{{ testResult.duration }} 分钟</el-descriptions-item>
        <el-descriptions-item label="路径点数">{{ testResult.path.length }} 个</el-descriptions-item>
        <el-descriptions-item label="步骤数">{{ testResult.steps.length }} 个</el-descriptions-item>
        <el-descriptions-item v-if="testResult.tolls > 0" label="收费">{{ testResult.tolls }} 元</el-descriptions-item>
        <el-descriptions-item v-if="testResult.tollDistance > 0" label="收费距离">{{ testResult.tollDistance.toFixed(2) }} 公里</el-descriptions-item>
      </el-descriptions>
      
      <div v-if="testResult.steps.length > 0" class="steps-preview">
        <h4>导航步骤预览</h4>
        <el-timeline>
          <el-timeline-item
            v-for="(step, index) in testResult.steps.slice(0, 5)"
            :key="index"
            :type="index === 0 ? 'success' : 'primary'"
          >
            <div class="step-preview">
              <div class="step-instruction">{{ step.instruction }}</div>
              <div class="step-info">
                <span>{{ (step.distance / 1000).toFixed(2) }}km</span>
                <span>{{ Math.round(step.duration / 60) }}分钟</span>
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
        <div v-if="testResult.steps.length > 5" class="more-steps">
          还有 {{ testResult.steps.length - 5 }} 个步骤...
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { useMapStore, type Station } from '../stores/mapStore';
import { pathPlanningService, type PathPlanningOptions } from '../services/pathPlanningService';

const mapStore = useMapStore();
const stations = ref<Station[]>([]);
const testing = ref(false);
const testResult = ref<any>(null);

const testConfig = ref({
  sourceId: '',
  targetId: '',
  vehicleType: 'bike' as 'car' | 'bike' | 'walk',
  optimizeFor: 'distance' as 'distance' | 'time'
});

const testPathPlanning = async () => {
  if (!testConfig.value.sourceId || !testConfig.value.targetId) {
    ElMessage.warning('请选择起点和终点站');
    return;
  }
  
  testing.value = true;
  
  try {
    // 初始化路径规划服务
    await pathPlanningService.initialize();
    
    // 获取起点和终点站
    const source = stations.value.find(s => s.id === testConfig.value.sourceId);
    const target = stations.value.find(s => s.id === testConfig.value.targetId);
    
    if (!source || !target) {
      ElMessage.error('站点信息不完整');
      return;
    }
    
    // 构建路径规划选项
    const options: PathPlanningOptions = {
      optimizeFor: testConfig.value.optimizeFor,
      vehicleType: testConfig.value.vehicleType
    };
    
    // 调用路径规划服务
    const result = await pathPlanningService.planPath(source, target, options);
    
    testResult.value = result;
    ElMessage.success('路径规划测试完成！');
    
  } catch (error) {
    console.error('路径规划测试失败:', error);
    ElMessage.error('路径规划测试失败，请检查网络连接');
  } finally {
    testing.value = false;
  }
};

onMounted(async () => {
  if (mapStore.stations.length === 0) {
    await mapStore.loadSubwayData();
  }
  stations.value = mapStore.stations;
});
</script>

<style scoped>
.path-test-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.test-card,
.result-card {
  margin-bottom: 20px;
}

.steps-preview {
  margin-top: 20px;
}

.step-preview {
  .step-instruction {
    font-weight: 500;
    margin-bottom: 4px;
  }
  
  .step-info {
    display: flex;
    gap: 12px;
    font-size: 12px;
    color: #909399;
  }
}

.more-steps {
  text-align: center;
  color: #909399;
  font-size: 12px;
  margin-top: 10px;
}
</style> 