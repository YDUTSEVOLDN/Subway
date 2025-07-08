<template>
  <div class="settings-container">
    <div class="page-header">
      <h2>系统设置</h2>
    </div>
    
    <el-card class="settings-card">
      <template #header>
        <div class="card-header">
          <span>基本设置</span>
        </div>
      </template>
      
      <el-form :model="baseSettings" label-position="top" :rules="rules" ref="baseSettingsForm">
        <el-form-item label="平台名称" prop="platformName">
          <el-input v-model="baseSettings.platformName" placeholder="输入平台名称"></el-input>
        </el-form-item>
        
        <el-form-item label="显示语言">
          <el-select v-model="baseSettings.language" placeholder="选择语言">
            <el-option label="简体中文" value="zh-CN"></el-option>
            <el-option label="English" value="en-US"></el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="默认城市">
          <el-select v-model="baseSettings.defaultCity" placeholder="选择默认城市">
            <el-option label="北京" value="beijing"></el-option>
            <el-option label="上海" value="shanghai"></el-option>
            <el-option label="广州" value="guangzhou"></el-option>
            <el-option label="深圳" value="shenzhen"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>
    
    <el-card class="settings-card">
      <template #header>
        <div class="card-header">
          <span>地图配置</span>
        </div>
      </template>
      
      <el-form :model="mapSettings" label-position="top" :rules="mapRules" ref="mapSettingsForm">
        <el-form-item label="百度地图 API 密钥" prop="baiduMapAk">
          <el-input 
            v-model="mapSettings.baiduMapAk" 
            placeholder="输入百度地图API密钥" 
            show-password
          ></el-input>
        </el-form-item>
        
        <el-form-item label="默认缩放级别" prop="defaultZoom">
          <el-slider 
            v-model="mapSettings.defaultZoom" 
            :min="10" 
            :max="19" 
            :step="1" 
            show-stops
            show-input
          ></el-slider>
        </el-form-item>
        
        <el-form-item label="地图类型">
          <el-radio-group v-model="mapSettings.mapType">
            <el-radio-button label="normal">普通地图</el-radio-button>
            <el-radio-button label="satellite">卫星地图</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </el-card>
    
    <el-card class="settings-card">
      <template #header>
        <div class="card-header">
          <span>数据分析设置</span>
        </div>
      </template>
      
      <el-form :model="dataSettings" label-position="top" ref="dataSettingsForm">
        <el-form-item label="数据刷新间隔 (秒)">
          <el-input-number 
            v-model="dataSettings.refreshInterval" 
            :min="10" 
            :max="600" 
            :step="10"
            controls-position="right"
          ></el-input-number>
        </el-form-item>
        
        <el-form-item label="高峰时段自动预警">
          <el-switch 
            v-model="dataSettings.enableAlerts" 
            active-text="启用" 
            inactive-text="禁用"
          ></el-switch>
        </el-form-item>
        
        <el-form-item label="数据可视化配色方案">
          <el-select v-model="dataSettings.colorScheme" placeholder="选择配色方案">
            <el-option label="默认" value="default"></el-option>
            <el-option label="暖色" value="warm"></el-option>
            <el-option label="冷色" value="cold"></el-option>
            <el-option label="高对比度" value="high-contrast"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>
    
    <div class="form-actions">
      <el-button @click="resetSettings">重置</el-button>
      <el-button type="primary" @click="saveSettings" :loading="saving">保存设置</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, type FormInstance } from 'element-plus';

// 表单引用
const baseSettingsForm = ref<FormInstance>();
const mapSettingsForm = ref<FormInstance>();
const dataSettingsForm = ref<FormInstance>();

// 基本设置
const baseSettings = reactive({
  platformName: '智慧交通监控与调度平台',
  language: 'zh-CN',
  defaultCity: 'beijing'
});

// 地图设置
const mapSettings = reactive({
  baiduMapAk: '',
  defaultZoom: 15,
  mapType: 'normal'
});

// 数据分析设置
const dataSettings = reactive({
  refreshInterval: 30,
  enableAlerts: true,
  colorScheme: 'default'
});

// 表单验证规则
const rules = {
  platformName: [
    { required: true, message: '请输入平台名称', trigger: 'blur' },
    { min: 3, max: 30, message: '长度在3到30个字符', trigger: 'blur' }
  ]
};

const mapRules = {
  baiduMapAk: [
    { required: true, message: '请输入百度地图API密钥', trigger: 'blur' },
    { min: 20, message: '密钥长度不正确', trigger: 'blur' }
  ],
  defaultZoom: [
    { type: 'number' as const, min: 10, max: 19, message: '缩放级别必须在10-19之间', trigger: 'change' }
  ]
};

// 状态
const saving = ref(false);

// 保存设置
const saveSettings = async () => {
  // 验证表单
  const validBase = await baseSettingsForm.value?.validate().catch(() => false);
  const validMap = await mapSettingsForm.value?.validate().catch(() => false);
  
  if (!validBase || !validMap) {
    ElMessage.error('请修正表单错误后再提交');
    return;
  }
  
  saving.value = true;
  
  try {
    // 模拟保存设置到服务器
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    // 保存到本地存储
    localStorage.setItem('baseSettings', JSON.stringify(baseSettings));
    localStorage.setItem('mapSettings', JSON.stringify(mapSettings));
    localStorage.setItem('dataSettings', JSON.stringify(dataSettings));
    
    ElMessage.success('设置保存成功');
  } catch (error) {
    console.error('保存设置失败', error);
    ElMessage.error('保存设置失败，请稍后重试');
  } finally {
    saving.value = false;
  }
};

// 重置设置
const resetSettings = () => {
  baseSettingsForm.value?.resetFields();
  mapSettingsForm.value?.resetFields();
  dataSettingsForm.value?.resetFields();
  
  // 重新加载存储的设置
  loadStoredSettings();
  
  ElMessage.info('已重置所有设置');
};

// 加载存储的设置
const loadStoredSettings = () => {
  try {
    // 基本设置
    const storedBaseSettings = localStorage.getItem('baseSettings');
    if (storedBaseSettings) {
      const parsedSettings = JSON.parse(storedBaseSettings);
      Object.assign(baseSettings, parsedSettings);
    }
    
    // 地图设置
    const storedMapSettings = localStorage.getItem('mapSettings');
    if (storedMapSettings) {
      const parsedSettings = JSON.parse(storedMapSettings);
      Object.assign(mapSettings, parsedSettings);
    }
    
    // 数据分析设置
    const storedDataSettings = localStorage.getItem('dataSettings');
    if (storedDataSettings) {
      const parsedSettings = JSON.parse(storedDataSettings);
      Object.assign(dataSettings, parsedSettings);
    }
  } catch (error) {
    console.error('加载存储的设置失败', error);
  }
};

// 组件挂载时加载设置
onMounted(() => {
  loadStoredSettings();
});
</script>

<style scoped lang="scss">
.settings-container {
  padding: 20px;
  
  .page-header {
    margin-bottom: 20px;
    
    h2 {
      margin: 0;
      font-size: 1.5rem;
    }
  }
  
  .settings-card {
    margin-bottom: 20px;
    
    .card-header {
      font-weight: bold;
      font-size: 16px;
    }
  }
  
  .form-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
    gap: 10px;
  }
}
</style> 