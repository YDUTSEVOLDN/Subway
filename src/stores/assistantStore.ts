import { defineStore } from 'pinia';
import { v4 as uuidv4 } from 'uuid';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
  type?: 'text' | 'path_planning_result';
  payload?: any;
}

export interface Conversation {
  id: string;
  title: string;
  messages: ChatMessage[];
  createdAt: number;
}

const STORAGE_KEY = 'chat-conversations';

const getConversationsFromStorage = (): Conversation[] => {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (error) {
    console.error('Failed to parse conversations from Local Storage', error);
  }
  return [];
};

const saveConversationsToStorage = (conversations: Conversation[]) => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(conversations));
  } catch (error) {
    console.error('Failed to save conversations to Local Storage', error);
  }
};

export const useAssistantStore = defineStore('assistant', {
  state: () => ({
    conversations: [] as Conversation[],
    activeConversationId: null as string | null,
  }),

  getters: {
    sortedConversations(state): Conversation[] {
      return [...state.conversations].sort((a, b) => b.createdAt - a.createdAt);
    },
    activeConversation(state): Conversation | undefined {
      return state.conversations.find(c => c.id === state.activeConversationId);
    },
  },

  actions: {
    loadConversations() {
      this.conversations = getConversationsFromStorage();
      if (this.conversations.length > 0) {
        // 默认激活最新的会话
        this.activeConversationId = this.sortedConversations[0].id;
      } else {
        // 如果没有历史记录，则创建一个新的
        this.createNewConversation();
      }
    },

    createNewConversation() {
      const newConversation: Conversation = {
        id: uuidv4(),
        title: '新的对话',
        messages: [],
        createdAt: Date.now(),
      };
      this.conversations.push(newConversation);
      this.activeConversationId = newConversation.id;
      saveConversationsToStorage(this.conversations);
    },

    setActiveConversation(id: string) {
      this.activeConversationId = id;
    },

    addMessageToActiveConversation(message: ChatMessage) {
      const conversation = this.activeConversation;
      if (conversation) {
        conversation.messages.push(message);
        // 如果是第一条用户消息，就尝试用它来生成标题
        if (conversation.messages.length === 1 && message.role === 'user') {
          // 截取前20个字符作为标题
          let newTitle = message.content.substring(0, 20);
          if (message.content.length > 20) newTitle += '...';
          conversation.title = newTitle;
        }
        saveConversationsToStorage(this.conversations);
      }
    },
    
    deleteConversation(id: string) {
      this.conversations = this.conversations.filter(c => c.id !== id);
      // 如果删除的是当前激活的对话
      if (this.activeConversationId === id) {
        if (this.conversations.length > 0) {
          this.activeConversationId = this.sortedConversations[0].id;
        } else {
          this.createNewConversation();
        }
      }
      saveConversationsToStorage(this.conversations);
    },
  },
}); 