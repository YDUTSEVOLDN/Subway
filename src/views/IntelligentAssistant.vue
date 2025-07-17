<template>
  <div class="assistant-page">
    <!-- Sidebar for conversation history -->
    <div class="sidebar">
      <div class="sidebar-header">
        <el-button type="primary" :icon="Plus" @click="assistantStore.createNewConversation" plain>
          新建对话
        </el-button>
      </div>
      <el-scrollbar class="conversation-list">
        <div 
          v-for="convo in assistantStore.sortedConversations" 
          :key="convo.id"
          class="conversation-item"
          :class="{ active: convo.id === assistantStore.activeConversationId }"
          @click="assistantStore.setActiveConversation(convo.id)"
        >
          <div class="convo-info">
            <el-icon class="convo-icon"><ChatLineSquare /></el-icon>
            <span class="convo-title">{{ convo.title }}</span>
          </div>
          <el-button 
            type="danger" 
            :icon="Delete" 
            circle 
            plain 
            size="small"
            class="delete-btn"
            @click.stop="deleteConversation(convo.id)"
          />
        </div>
      </el-scrollbar>
    </div>

    <!-- Main chat area -->
    <div class="chat-area">
      <div class="assistant-container">
        <div class="assistant-header">
          <h2>{{ assistantStore.activeConversation?.title || '智能助手' }}</h2>
        </div>
        <div class="chat-container" ref="chatContainerRef">
          <div class="welcome-message" v-if="!activeMessages || activeMessages.length === 0">
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
              v-for="(message, index) in activeMessages" 
              :key="index" 
              :class="['message', message.role === 'user' ? 'user-message' : 'assistant-message']"
            >
              <div class="message-avatar">
                <el-avatar :size="36" :src="message.role === 'user' ? userAvatar : '/logo.svg'"/>
              </div>
              <div class="message-content">
                <div class="message-header">
                  <span class="message-sender">{{ message.role === 'user' ? '我' : '智能助手' }}</span>
                  <span class="message-time">{{ formatTime(message.timestamp) }}</span>
                </div>
                <div class="message-body" v-html="formatMessage(message.content)"></div>
                 <div v-if="message.type === 'path_planning_result'" class="action-button-container">
                  <el-button 
                    type="primary" 
                    :icon="Position" 
                    plain
                    @click="goToPathPlanner(message.payload)"
                  >
                    跳转到路径规划
                  </el-button>
                </div>
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
                  <span></span><span></span><span></span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="input-container">
          <el-input v-model="userInput" type="textarea" :rows="3" placeholder="请输入您的问题或指令..." resize="none" @keyup.enter.ctrl="sendMessage"/>
          <div class="input-actions">
            <span class="input-tip">按 Ctrl+Enter 发送</span>
            <el-button type="primary" :icon="Position" :disabled="!userInput.trim() || isLoading" @click="sendMessage">发送</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ChatDotRound, Delete, Position, Plus, ChatLineSquare } from '@element-plus/icons-vue';
import { useUserStore } from '../stores/userStore';
import { useAssistantStore, type ChatMessage } from '../stores/assistantStore';
import { llmService } from '../services/llmService';
import { marked } from 'marked';
import DOMPurify from 'dompurify';

const router = useRouter();
const userStore = useUserStore();
const assistantStore = useAssistantStore();

const userAvatar = computed(() => userStore.user?.avatar || '/default-avatar.svg');
const activeMessages = computed(() => assistantStore.activeConversation?.messages);

const userInput = ref('');
const isLoading = ref(false);
const chatContainerRef = ref<HTMLElement | null>(null);

onMounted(() => {
  assistantStore.loadConversations();
  scrollToBottom();
});

watch(() => assistantStore.activeConversationId, () => {
  nextTick(scrollToBottom);
});

watch(activeMessages, () => {
  nextTick(scrollToBottom);
}, { deep: true });

const useQuickQuestion = (question: string) => {
  userInput.value = question;
  sendMessage();
};

const sendMessage = async () => {
  const messageText = userInput.value.trim();
  if (!messageText || isLoading.value) return;

  const userMessage: ChatMessage = { role: 'user', content: messageText, timestamp: Date.now() };
  assistantStore.addMessageToActiveConversation(userMessage);
  
  userInput.value = '';
  isLoading.value = true;
  await nextTick();
  scrollToBottom();

  try {
    const assistantResponse = await llmService.processNaturalLanguageQuery(messageText);
    const assistantMessage: ChatMessage = {
      role: 'assistant',
      content: assistantResponse.content,
      type: assistantResponse.type,
      payload: assistantResponse.payload,
      timestamp: Date.now()
    };
    assistantStore.addMessageToActiveConversation(assistantMessage);
  } catch (error) {
    console.error('聊天服务调用失败:', error);
    const errorMessage: ChatMessage = {
      role: 'assistant',
      content: '抱歉，处理您的请求时出现了一些问题。请稍后再试。',
      timestamp: Date.now()
    };
    assistantStore.addMessageToActiveConversation(errorMessage);
  } finally {
    isLoading.value = false;
    await nextTick();
    scrollToBottom();
  }
};

const deleteConversation = (id: string) => {
  ElMessageBox.confirm('确定要删除这个对话吗？此操作不可撤销。', '确认删除', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    assistantStore.deleteConversation(id);
    ElMessage.success('对话已删除');
  }).catch(() => {});
};

const goToPathPlanner = (payload: any) => {
  if (!payload) return;
  router.push({ name: 'path-planner', query: {
    start: payload.startStationName,
    end: payload.endStationName,
    count: payload.bikeCount
  }});
};

const formatMessage = (message: string) => DOMPurify.sanitize(marked(message));
const formatTime = (timestamp: number) => new Date(timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
const scrollToBottom = () => {
  if (chatContainerRef.value) {
    chatContainerRef.value.scrollTop = chatContainerRef.value.scrollHeight;
  }
};
</script>

<style scoped lang="scss">
.assistant-page {
  display: flex;
  height: calc(100vh - 80px); /* Adjust based on your layout's header/footer */
  background-color: #f0f2f5;
}

.sidebar {
  width: 260px;
  background-color: #fff;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;

  .sidebar-header {
    padding: 16px;
    border-bottom: 1px solid #e0e0e0;

    .el-button {
      width: 100%;
    }
  }

  .conversation-list {
    flex: 1;
    .conversation-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 16px;
      cursor: pointer;
      border-bottom: 1px solid #f5f5f5;
      transition: background-color 0.2s;

      &:hover {
        background-color: #f5f7fa;
      }

      &.active {
        background-color: #ecf5ff;
        color: #409eff;
      }

      .convo-info {
        display: flex;
        align-items: center;
        gap: 8px;
        overflow: hidden;
        white-space: nowrap;

        .convo-title {
          text-overflow: ellipsis;
          overflow: hidden;
        }
      }

      .delete-btn {
        visibility: hidden;
        opacity: 0;
        transition: opacity 0.2s;
      }
      
      &:hover .delete-btn {
        visibility: visible;
        opacity: 1;
      }
    }
  }
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
}

.assistant-container {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow: hidden;
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
      .action-button-container {
        margin-top: 12px;
        padding-top: 12px;
        border-top: 1px solid #ebeef5;
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