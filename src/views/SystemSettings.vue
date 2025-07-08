<template>
  <div class="settings-container">
    <h2>系统设置</h2>
    
    <el-card class="settings-card">
      <template #header>
        <div class="card-header">
          <span>地图配置</span>
        </div>
      </template>
      
      <el-form :model="mapSettings" label-position="top">
        <el-form-item label="高德地图 API 密钥" required>
          <el-input 
            v-model="mapSettings.apiKey" 
            placeholder="输入高德地图API密钥"
            :show-password="true"
          />
          <div class="form-item-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>请在<a href="https://lbs.amap.com/" target="_blank">高德地图开放平台</a>申请API密钥</span>
          </div>
        </el-form-item>
        
        <el-form-item label="默认缩放级别">
          <el-slider 
            v-model="mapSettings.defaultZoom" 
            :min="3" 
            :max="19" 
            :step="1" 
            show-stops
          />
          <div class="zoom-labels">
            <span>远</span>
            <span>近</span>
          </div>
        </el-form-item>
        
        <el-form-item label="地图类型">
          <el-radio-group v-model="mapSettings.mapType">
            <el-radio-button label="normal">普通地图</el-radio-button>
            <el-radio-button label="satellite">卫星地图</el-radio-button>
          </el-radio-group>
        </el-form-item>
        
        <el-divider />
        
        <el-form-item>
          <el-button type="primary" @click="saveSettings" :loading="saving">保存设置</el-button>
          <el-button @click="resetSettings">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <el-card class="settings-card">
      <template #header>
        <div class="card-header">
          <span>系统信息</span>
        </div>
      </template>
      
      <el-descriptions :column="1" border>
        <el-descriptions-item label="系统版本">v1.0.0</el-descriptions-item>
        <el-descriptions-item label="前端框架">Vue 3 + Vite + TypeScript</el-descriptions-item>
        <el-descriptions-item label="UI组件库">Element Plus</el-descriptions-item>
        <el-descriptions-item label="地图引擎">高德地图 JavaScript API</el-descriptions-item>
        <el-descriptions-item label="图表库">Apache ECharts</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { InfoFilled } from '@element-plus/icons-vue';
import { mapConfig } from '../config/map';

// 地图设置
const mapSettings = ref({
  apiKey: '',
  defaultZoom: 13,
  mapType: 'normal'
});

const saving = ref(false);

// 加载设置
const loadSettings = () => {
  // 从本地存储加载设置
  const savedSettings = localStorage.getItem('mapSettings');
  if (savedSettings) {
    try {
      const settings = JSON.parse(savedSettings);
      mapSettings.value = {
        ...mapSettings.value,
        ...settings
      };
    } catch (e) {
      console.error('解析地图设置失败:', e);
    }
  } else {
    // 使用默认配置
    mapSettings.value.apiKey = mapConfig.amapKey || '';
    mapSettings.value.defaultZoom = mapConfig.defaultZoom || 13;
  }
};

// 保存设置
const saveSettings = async () => {
  if (!mapSettings.value.apiKey) {
    ElMessage.warning('请输入高德地图API密钥');
    return;
  }
  
  saving.value = true;
  
  try {
    // 保存到本地存储
    localStorage.setItem('mapSettings', JSON.stringify(mapSettings.value));
    
    // 实际项目中，这里可能需要调用API保存到后端
    await new Promise(resolve => setTimeout(resolve, 500)); // 模拟API延迟
    
    ElMessage.success('设置保存成功，请刷新页面以应用新设置');
    
    // 更新当前页面的API密钥
    if (window.AMap) {
      console.log('地图API已加载，需要刷新页面应用新密钥');
    }
  } catch (error) {
    console.error('保存设置失败:', error);
    ElMessage.error('保存设置失败');
  } finally {
    saving.value = false;
  }
};

// 重置设置
const resetSettings = () => {
  mapSettings.value.apiKey = mapConfig.amapKey || '';
  mapSettings.value.defaultZoom = mapConfig.defaultZoom || 13;
  mapSettings.value.mapType = 'normal';
};

// 组件挂载
onMounted(() => {
  loadSettings();
});
</script>

<style scoped lang="scss">
.settings-container {
  padding: 20px;
  
  h2 {
    margin-bottom: 20px;
  }
  
  .settings-card {
    margin-bottom: 20px;
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }
  
  .form-item-tip {
    margin-top: 8px;
    font-size: 12px;
    color: #909399;
    display: flex;
    align-items: center;
    
    .el-icon {
      margin-right: 4px;
    }
    
    a {
      color: #409EFF;
      margin-left: 4px;
      text-decoration: none;
      
      &:hover {
        text-decoration: underline;
      }
    }
  }
  
  .zoom-labels {
    display: flex;
    justify-content: space-between;
    margin-top: 8px;
    color: #909399;
    font-size: 12px;
  }
}
</style> 