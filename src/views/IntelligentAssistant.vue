<template>
  <div class="assistant-container">
    <div class="assistant-header">
      <h2>智能助手</h2>
      <el-tooltip content="清空当前对话" placement="top">
        <el-button 
          type="danger" 
          :icon="Delete" 
          circle 
          plain 
          size="small" 
          @click="clearConversation" 
        />
      </el-tooltip>
    </div>

    <div class="chat-container" ref="chatContainerRef">
      <div class="welcome-message" v-if="messages.length === 0">
        <div class="welcome-icon">
          <el-icon :size="40"><ChatDotRound /></el-icon>
        </div>
        <h3>欢迎使用智能助手</h3>
        <p>我可以帮助您查询系统数据、执行常见操作和分析趋势。</p>
        <div class="suggestion-buttons">
          <el-button @click="useQuickQuestion('今天哪个站点的单车缺口最大？')">🚲 哪个站点单车缺口最大？</el-button>
          <el-button @click="useQuickQuestion('帮我规划一条从西单到国贸的调度路线，需要20辆车')">🗺️ 规划西单到国贸的调度路线</el-button>
          <el-button @click="useQuickQuestion('对比一下五道口和中关村上周的客流量趋势')">📊 对比五道口和中关村客流量</el-button>
        </div>
      </div>

      <div v-else class="messages">
        <div 
          v-for="(message, index) in messages" 
          :key="index" 
          :class="['message', message.role === 'user' ? 'user-message' : 'assistant-message']"
        >
          <div class="message-avatar">
            <el-avatar 
              :size="36" 
              :src="message.role === 'user' ? userAvatar : '/logo.svg'"
            />
          </div>
          <div class="message-content">
            <div class="message-header">
              <span class="message-sender">{{ message.role === 'user' ? '我' : '智能助手' }}</span>
              <span class="message-time">{{ formatTime(message.timestamp) }}</span>
            </div>
            <div class="message-body" v-html="formatMessage(message.content)"></div>
          </div>
        </div>
        
        <div v-if="isLoading" class="message assistant-message">
          <div class="message-avatar">
            <el-avatar :size="36" :src="'/logo.svg'" />
          </div>
          <div class="message-content">
            <div class="message-header">
              <span class="message-sender">智能助手</span>
            </div>
            <div class="message-body typing-indicator">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="input-container">
      <el-input
        v-model="userInput"
        type="textarea"
        :rows="3"
        placeholder="请输入您的问题或指令..."
        resize="none"
        @keyup.enter.ctrl="sendMessage"
      />
      <div class="input-actions">
        <span class="input-tip">按 Ctrl+Enter 发送</span>
        <el-button 
          type="primary" 
          :icon="Position" 
          :disabled="!userInput.trim() || isLoading" 
          @click="sendMessage"
        >
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ChatDotRound, Delete, Position } from '@element-plus/icons-vue';
import { useUserStore } from '../stores/userStore';
import { llmService, type ChatMessage } from '../services/llmService';
import { marked } from 'marked';
import DOMPurify from 'dompurify';

// 用户信息
const userStore = useUserStore();
const userAvatar = computed(() => {
  return userStore.user?.avatar || '/default-avatar.svg';
});

// 聊天相关状态
const userInput = ref('');
const messages = ref<Array<{role: string, content: string, timestamp: number}>>([]);
const isLoading = ref(false);
const chatContainerRef = ref<HTMLElement | null>(null);

// 快速问题
const useQuickQuestion = (question: string) => {
  userInput.value = question;
  sendMessage();
};

// 发送消息
const sendMessage = async () => {
  const message = userInput.value.trim();
  if (!message || isLoading.value) return;
  
  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: message,
    timestamp: Date.now()
  });
  
  userInput.value = '';
  isLoading.value = true;
  
  // 滚动到底部
  await nextTick();
  scrollToBottom();
  
  try {
    // 准备消息历史
    const chatMessages: ChatMessage[] = messages.value.map(msg => ({
      role: msg.role as 'user' | 'assistant',
      content: msg.content
    }));
    
    // 调用服务
    const response = await llmService.sendChatMessage(chatMessages);
    
    messages.value.push({
      role: 'assistant',
      content: response.content,
      timestamp: Date.now()
    });
  } catch (error) {
    console.error('聊天服务调用失败:', error);
    // 使用模拟数据
    const mockResponses = generateMockResponse(message);
    messages.value.push({
      role: 'assistant',
      content: mockResponses,
      timestamp: Date.now()
    });
  } finally {
    isLoading.value = false;
    await nextTick();
    scrollToBottom();
  }
};

// 生成模拟响应
const generateMockResponse = (message: string): string => {
  // 模拟三种类型的回复：事实性问答、指令式操作、分析式请求
  const lowerMsg = message.toLowerCase();
  
  // 事实性问答
  if (lowerMsg.includes('哪个站点') || lowerMsg.includes('多少')) {
    if (lowerMsg.includes('缺口最大') || lowerMsg.includes('最短缺')) {
      return '截至目前，单车缺口最大的站点是**西二旗站**，缺少约**58**辆车。\n\n这主要是由于早高峰期间该站点周边科技园区员工出行需求激增，而补给不及时导致的。建议您可以：\n\n1. 在地图上查看该站点的详细状态\n2. 创建一个调度计划来缓解短缺\n\n需要我为您创建一个调度计划吗？';
    }
    return '根据系统监测数据，目前：\n\n- 单车富余最多的站点：**北京南站**，富余**47**辆车\n- 单车短缺最严重的站点：**西二旗站**，缺少**58**辆车\n- 客流量最大的站点：**北京站**，日均客流**42,586**人次\n\n您需要查看更详细的站点数据吗？';
  }
  
  // 指令式操作
  if (lowerMsg.includes('规划') || lowerMsg.includes('调度')) {
    let source = '西单';
    let target = '国贸';
    let count = '20';
    
    // 尝试提取站点名称和数量
    const stationMatch = message.match(/从([\u4e00-\u9fa5]+)到([\u4e00-\u9fa5]+)/);
    if (stationMatch) {
      source = stationMatch[1];
      target = stationMatch[2];
    }
    
    const countMatch = message.match(/(\d+)辆车/);
    if (countMatch) {
      count = countMatch[1];
    }
    
    return `我可以帮您规划从**${source}**到**${target}**的调度路线，调度**${count}**辆单车。\n\n这将需要跳转到路径规划页面并自动填充这些参数。实际项目中，我会通过Function Calling调用路径规划功能。\n\n您想查看推荐的调度时间吗？根据历史数据分析，在下午3点进行这条线路的调度效率最高。`;
  }
  
  // 分析式请求
  if (lowerMsg.includes('对比') || lowerMsg.includes('趋势') || lowerMsg.includes('分析')) {
    let station1 = '五道口';
    let station2 = '中关村';
    let timeRange = '上周';
    
    // 尝试提取站点名称和时间范围
    const stationsMatch = message.match(/([\u4e00-\u9fa5]+)和([\u4e00-\u9fa5]+)/);
    if (stationsMatch) {
      station1 = stationsMatch[1];
      station2 = stationsMatch[2];
    }
    
    if (lowerMsg.includes('本月')) {
      timeRange = '本月';
    } else if (lowerMsg.includes('上月')) {
      timeRange = '上月';
    }
    
    return `**${station1}**和**${station2}**${timeRange}客流量趋势对比分析：\n\n1. **高峰期对比**：${station1}的早高峰比${station2}早30分钟开始，但峰值客流量低20%\n2. **周末表现**：${station2}周末客流稳定，而${station1}周末下降明显（-35%）\n3. **增长趋势**：${station1}客流量环比增长12%，${station2}基本持平（+2%）\n\n在实际系统中，我会跳转到数据分析页面并自动生成这两个站点的对比图表。需要我帮您查看更具体的某一天的数据吗？`;
  }
  
  // 默认回复
  return `感谢您的问题！作为交通调度系统的智能助手，我可以帮您：\n\n- 查询站点数据（客流、单车状态等）\n- 创建和管理调度计划\n- 分析历史趋势和模式\n\n请问您想了解哪方面的信息？或者需要我执行什么具体任务？`;
};

// 格式化消息
const formatMessage = (message: string): string => {
  // 使用marked解析Markdown，并使用DOMPurify净化HTML
  return DOMPurify.sanitize(marked(message));
};

// 滚动到底部
const scrollToBottom = () => {
  if (chatContainerRef.value) {
    chatContainerRef.value.scrollTop = chatContainerRef.value.scrollHeight;
  }
};

// 格式化时间
const formatTime = (timestamp: number): string => {
  const date = new Date(timestamp);
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
};

// 清空对话
const clearConversation = () => {
  ElMessageBox.confirm('确定要清空当前对话吗？此操作不可撤销', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    messages.value = [];
    ElMessage.success('对话已清空');
  }).catch(() => {});
};

// 监听消息变化，滚动到底部
watch(messages, () => {
  nextTick(() => {
    scrollToBottom();
  });
}, { deep: true });

// 组件挂载时，滚动到底部
onMounted(() => {
  scrollToBottom();
});
</script>

<style scoped lang="scss">
.assistant-container {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  overflow: hidden;
  margin: 16px;
}

.assistant-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: #fff;
  border-bottom: 1px solid #ebeef5;
  
  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #303133;
  }
}

.chat-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  
  .welcome-message {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    text-align: center;
    padding: 0 20px;
    
    .welcome-icon {
      display: flex;
      justify-content: center;
      align-items: center;
      width: 80px;
      height: 80px;
      background: linear-gradient(135deg, #409eff, #3367d6);
      border-radius: 50%;
      margin-bottom: 16px;
      color: white;
    }
    
    h3 {
      font-size: 24px;
      font-weight: 600;
      margin: 0 0 12px 0;
      color: #303133;
    }
    
    p {
      font-size: 16px;
      color: #606266;
      margin: 0 0 24px 0;
      max-width: 600px;
    }
    
    .suggestion-buttons {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 12px;
      margin-top: 16px;
    }
  }
  
  .messages {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  
  .message {
    display: flex;
    gap: 12px;
    max-width: 85%;
    
    &.user-message {
      align-self: flex-end;
      flex-direction: row-reverse;
      
      .message-content {
        background-color: #ecf5ff;
        border: 1px solid #d9ecff;
        
        .message-header {
          flex-direction: row-reverse;
        }
        
        .message-body {
          color: #409eff;
        }
      }
    }
    
    &.assistant-message {
      align-self: flex-start;
      
      .message-content {
        background-color: #fff;
        border: 1px solid #ebeef5;
      }
    }
    
    .message-avatar {
      flex-shrink: 0;
    }
    
    .message-content {
      padding: 12px;
      border-radius: 8px;
      overflow: hidden;
      
      .message-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 6px;
        
        .message-sender {
          font-weight: 600;
          font-size: 14px;
          color: #303133;
        }
        
        .message-time {
          font-size: 12px;
          color: #909399;
        }
      }
      
      .message-body {
        font-size: 14px;
        line-height: 1.5;
        word-break: break-word;
        color: #606266;
        
        p:first-child {
          margin-top: 0;
        }
        
        p:last-child {
          margin-bottom: 0;
        }
      }
    }
  }
  
  .typing-indicator {
    display: flex;
    padding: 8px 0;
    
    span {
      display: inline-block;
      width: 8px;
      height: 8px;
      margin-right: 5px;
      background-color: #606266;
      border-radius: 50%;
      animation: typing 1.5s infinite ease-in-out;
      
      &:nth-child(1) {
        animation-delay: 0s;
      }
      
      &:nth-child(2) {
        animation-delay: 0.2s;
      }
      
      &:nth-child(3) {
        animation-delay: 0.4s;
        margin-right: 0;
      }
    }
  }
}

.input-container {
  padding: 16px;
  background-color: #fff;
  border-top: 1px solid #ebeef5;
  
  .input-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 10px;
    
    .input-tip {
      font-size: 12px;
      color: #909399;
    }
  }
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-6px);
  }
}

:deep(.message-body) {
  strong, b {
    color: #303133;
    font-weight: 600;
  }
  
  ul, ol {
    padding-left: 20px;
    margin: 8px 0;
  }
  
  code {
    background-color: #f5f7fa;
    padding: 2px 4px;
    border-radius: 4px;
    font-family: monospace;
  }
  
  pre {
    background-color: #f5f7fa;
    padding: 10px;
    border-radius: 4px;
    overflow-x: auto;
    margin: 10px 0;
  }
}
</style> 