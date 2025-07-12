<template>
  <div class="time-simulator">
    <el-card shadow="hover" class="time-card">
      <template #header>
        <div class="card-header">
          <span>时间模拟器</span>
          <div class="header-actions">
            <el-switch
              v-model="isAutoPlay"
              inline-prompt
              active-text="自动"
              inactive-text="手动"
              @change="handleAutoPlayChange"
            />
          </div>
        </div>
      </template>
      
      <div class="time-display">
        <div class="time">{{ formattedTime }}</div>
        <div class="date">{{ formattedDate }}</div>
      </div>
      
      <div class="time-controls">
        <el-slider
          v-model="hourOfDay"
          :min="0"
          :max="23"
          :format-tooltip="formatTooltip"
          :disabled="isAutoPlay"
          @update:model-value="updateCurrentTime"
        />
        
        <div class="time-actions">
          <el-button-group>
            <el-button :disabled="isAutoPlay" size="small" @click="adjustTime(-1)">
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
            <el-button :disabled="isAutoPlay" size="small" @click="adjustTime(1)">
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </el-button-group>
          
          <el-button
            :icon="isAutoPlay ? 'Pause' : 'VideoPlay'"
            type="primary"
            size="small"
            @click="toggleAutoPlay"
          >
            {{ isAutoPlay ? '暂停' : '播放' }}
          </el-button>
          
          <el-tooltip content="重置到当前时间">
            <el-button 
              size="small" 
              @click="resetToCurrentTime"
              :icon="'Refresh'"
            />
          </el-tooltip>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue';
import { useMapStore } from '../../stores/mapStore';
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue';

// 引入地图存储
const mapStore = useMapStore();

// 当前模拟时间
const currentTime = ref(new Date());
// 一天中的小时数 (0-23)
const hourOfDay = ref(new Date().getHours());
// 是否自动播放
const isAutoPlay = ref(false);
// 自动播放计时器ID
let autoPlayTimerId: number | null = null;

// 格式化时间显示
const formattedTime = computed(() => {
  const hours = currentTime.value.getHours().toString().padStart(2, '0');
  const minutes = currentTime.value.getMinutes().toString().padStart(2, '0');
  return `${hours}:${minutes}`;
});

// 格式化日期显示
const formattedDate = computed(() => {
  const year = currentTime.value.getFullYear();
  const month = (currentTime.value.getMonth() + 1).toString().padStart(2, '0');
  const day = currentTime.value.getDate().toString().padStart(2, '0');
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
  const weekday = weekdays[currentTime.value.getDay()];
  return `${year}-${month}-${day} ${weekday}`;
});

// 格式化滑块提示文本
const formatTooltip = (val: number) => {
  return `${val.toString().padStart(2, '0')}:00`;
};

// 更新当前时间
const updateCurrentTime = (hour: number | number[]) => {
  const newTime = new Date();
  // 保持原来的年月日
  newTime.setHours(typeof hour === 'number' ? hour : (Array.isArray(hour) ? hour[0] : 0));
  newTime.setMinutes(0);
  newTime.setSeconds(0);
  currentTime.value = newTime;
  
  // 同步到地图存储
  mapStore.setCurrentTime(newTime);
};

// 调整时间（前进或后退）
const adjustTime = (hours: number) => {
  hourOfDay.value = (hourOfDay.value + hours + 24) % 24;
  updateCurrentTime(hourOfDay.value);
};

// 切换自动播放状态
const toggleAutoPlay = () => {
  isAutoPlay.value = !isAutoPlay.value;
  handleAutoPlayChange(isAutoPlay.value);
};

// 自动播放状态变化处理
const handleAutoPlayChange = (isAuto: boolean | string | number) => {
  if (isAuto === true) {
    startAutoPlay();
  } else {
    stopAutoPlay();
  }
};

// 开始自动播放
const startAutoPlay = () => {
  if (autoPlayTimerId !== null) return;
  
  autoPlayTimerId = window.setInterval(() => {
    // 每隔一定时间前进一小时
    adjustTime(1);
  }, 3000); // 3秒切换一次
};

// 停止自动播放
const stopAutoPlay = () => {
  if (autoPlayTimerId !== null) {
    clearInterval(autoPlayTimerId);
    autoPlayTimerId = null;
  }
};

// 重置到当前实时时间
const resetToCurrentTime = () => {
  const now = new Date();
  hourOfDay.value = now.getHours();
  updateCurrentTime(hourOfDay.value);
};

// 监听小时变化，更新当前时间
watch(hourOfDay, (newVal) => {
  updateCurrentTime(newVal);
});

// 组件挂载时初始化
onMounted(() => {
  updateCurrentTime(hourOfDay.value);
});

// 组件卸载时清理定时器
onBeforeUnmount(() => {
  stopAutoPlay();
});
</script>

<style scoped lang="scss">
.time-simulator {
  width: 100%;
  
  .time-card {
    border-radius: 8px;
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .header-actions {
        display: flex;
        gap: 8px;
      }
    }
  }
  
  .time-display {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-bottom: 16px;
    
    .time {
      font-size: 28px;
      font-weight: bold;
      line-height: 1.2;
    }
    
    .date {
      font-size: 14px;
      color: #606266;
    }
  }
  
  .time-controls {
    .time-actions {
      display: flex;
      justify-content: center;
      margin-top: 16px;
      gap: 12px;
    }
  }
}
</style> 