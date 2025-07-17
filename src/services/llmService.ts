import axios from 'axios';
import type { Station } from '../stores/mapStore';
import { useMapStore } from '@/stores/mapStore';
import { bikeService } from './bikeService';
import { pathPlanningService } from './pathPlanningService';
import trafficService from './trafficService';

interface LLMDispatchSummaryRequest {
  sourceStation: {
    name: string;
    position: { lng: number; lat: number };
    status: 'surplus' | 'shortage' | 'balanced';
  };
  targetStation: {
    name: string;
    position: { lng: number; lat: number };
    status: 'surplus' | 'shortage' | 'balanced';
  };
  bikeCount: number;
  distance: number; // 米
  duration: number; // 分钟
  roadNames?: string[]; // 可选，主要道路名称
  scheduledTime?: string; // 可选，计划执行时间
}

export interface LLMDispatchSummaryResponse {
  title: string;
  summary: string;
  riskHint: string;
  executionPlan?: string; // 详细执行计划
  trafficAnalysis?: string; // 交通分析
  resourceRequirements?: string; // 资源需求
  expectedBenefits?: string; // 预期效益
  alternativeRoutes?: string; // 备选路线
}

// 聊天功能相关接口
export interface ChatMessage {
  role: 'system' | 'user' | 'assistant';
  content: string;
}

export interface ChatCompletionRequest {
  messages: ChatMessage[];
}

export interface ChatCompletionResponse {
  content: string;
}

// 定义助手的标准响应格式
export interface AssistantResponse {
  type: 'text' | 'path_planning_result';
  content: string; // LLM生成的自然语言回复
  payload?: { // 可供前端执行操作的结构化数据
    startStationName: string;
    endStationName: string;
    bikeCount: number;
  };
}


export class LLMService {
  private apiKey: string;
  private baseUrl: string = 'https://api.deepseek.com/v1';

  constructor(apiKey: string) {
    this.apiKey = apiKey;
  }

  async processNaturalLanguageQuery(query: string): Promise<AssistantResponse> {
    const tools = [
      {
        type: 'function',
        function: {
          name: 'get_station_shortage_ranking',
          description: '获取当前所有地铁站中，单车最紧缺或最富余的站点排名。',
          parameters: {
            type: 'object',
            properties: {
              top_n: { type: 'number', description: '需要返回的排名数量，例如前5名。' },
              order: { type: 'string', enum: ['shortage', 'surplus'], description: '查询短缺排名还是富余排名。' }
            },
            required: ['top_n', 'order']
          }
        }
      },
      {
        type: 'function',
        function: {
          name: 'plan_dispatch_route',
          description: '为两个指定的地铁站规划单车调度路线。',
          parameters: {
            type: 'object',
            properties: {
              start_station_name: { type: 'string', description: '起点的地铁站名称，例如"西单"。' },
              end_station_name: { type: 'string', description: '终点的地铁站名称，例如"国贸"。' },
              bike_count: { type: 'number', description: '计划调度的单车数量。' }
            },
            required: ['start_station_name', 'end_station_name', 'bike_count']
          }
        }
      },
      {
        type: 'function',
        function: {
          name: 'compare_station_traffic',
          description: '对比两个指定地铁站最近一段时间的客流量趋势。',
          parameters: {
            type: 'object',
            properties: {
              station_a_name: { type: 'string', description: '第一个需要对比的地铁站名称。' },
              station_b_name: { type: 'string', description: '第二个需要对比的地铁站名称。' },
            },
            required: ['station_a_name', 'station_b_name']
          }
        }
      }
    ];

    try {
      const response = await axios.post(
        `${this.baseUrl}/chat/completions`,
        {
          model: 'deepseek-chat',
          messages: [
            { role: 'system', content: '你是一个智能交通调度平台的AI助手。你的任务是理解用户的自然语言查询，并使用提供的工具来调用系统内部功能以获取真实数据。然后根据获取到的数据，以友好、清晰的方式回答用户。' },
            { role: 'user', content: query }
          ],
          tools: tools,
          tool_choice: 'auto'
        },
        {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${this.apiKey}`
          }
        }
      );

      const message = response.data.choices[0].message;

      if (message.tool_calls && message.tool_calls.length > 0) {
        const toolCall = message.tool_calls[0];
        const functionName = toolCall.function.name;
        const args = JSON.parse(toolCall.function.arguments);

        let functionResult;

        switch (functionName) {
          case 'get_station_shortage_ranking':
            functionResult = await this.execute_get_station_shortage_ranking(args.top_n, args.order);
            break;
          case 'plan_dispatch_route':
            functionResult = await this.execute_plan_dispatch_route(args.start_station_name, args.end_station_name, args.bike_count);
            break;
          case 'compare_station_traffic':
            functionResult = await this.execute_compare_station_traffic(args.station_a_name, args.station_b_name);
            break;
          default:
            return { type: 'text', content: '抱歉，我无法执行一个未知的工具。' };
        }
        
        // After getting the functionResult, before calling the second LLM
        if (functionName === 'plan_dispatch_route' && functionResult.success) {
          // If the tool was path planning and it succeeded,
          // we now have enough info to create a structured response.
          // Let's get the text summary from the LLM.

          const secondResponse = await this.getFinalAnswer(query, message, toolCall.id, functionResult);

          return {
            type: 'path_planning_result',
            content: secondResponse,
            payload: {
              startStationName: args.start_station_name,
              endStationName: args.end_station_name,
              bikeCount: args.bike_count,
            }
          };
        }
        
        // For other tools, just get the text summary
        const summary = await this.getFinalAnswer(query, message, toolCall.id, functionResult);
        return { type: 'text', content: summary };
      } else {
        // No tool call, just a text response
        return { type: 'text', content: message.content };
      }
    } catch (error) {
      console.error('处理自然语言查询时出错:', error);
      return { type: 'text', content: '抱歉，我在处理您的请求时遇到了一个内部错误。' };
    }
  }
  
  // Helper function to get final LLM summary
  private async getFinalAnswer(query: string, previousMessage: any, toolCallId: string, toolResult: any): Promise<string> {
    const response = await axios.post(
      `${this.baseUrl}/chat/completions`,
      {
        model: 'deepseek-chat',
        messages: [
          { role: 'user', content: query },
          previousMessage,
          {
            role: 'tool',
            tool_call_id: toolCallId,
            content: JSON.stringify(toolResult)
          }
        ]
      },
      { headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${this.apiKey}` } }
    );
    return response.data.choices[0].message.content;
  }

  // --- Tool Execution Functions ---

  private async execute_get_station_shortage_ranking(top_n: number, order: 'shortage' | 'surplus'): Promise<any> {
    const mapStore = useMapStore();
    await mapStore.fetchStations(); // 确保站点数据已加载

    const statuses = await bikeService.getBikeStatusForAllStations();
    
    if (!statuses || statuses.length === 0) {
      return { error: '无法获取站点状态数据，请检查后端服务或数据源。' };
    }

    const processed = statuses.map(s => ({
      name: s.name,
      diff: s.supply - s.demand,
      supply: s.supply,
      demand: s.demand,
    }));

    if (order === 'shortage') {
      processed.sort((a, b) => a.diff - b.diff);
      return { ranking_type: 'shortage', result: processed.slice(0, top_n) };
    } else {
      processed.sort((a, b) => b.diff - a.diff);
      return { ranking_type: 'surplus', result: processed.slice(0, top_n) };
    }
  }

  private async execute_plan_dispatch_route(start_station_name: string, end_station_name: string, bike_count: number): Promise<any> {
    const mapStore = useMapStore();
    await mapStore.fetchStations(); // 确保站点数据已加载

    const startStation = mapStore.findStationByName(start_station_name);
    const endStation = mapStore.findStationByName(end_station_name);

    if (!startStation || !endStation) {
      return { error: `无法找到站点: ${!startStation ? start_station_name : ''} ${!endStation ? end_station_name : ''}`.trim() };
    }
    
    await pathPlanningService.initialize();
    const result = await pathPlanningService.planPath(startStation, endStation, { vehicleType: 'bike', optimizeFor: 'distance' });

    if (!result) {
      return { error: `无法规划从 ${start_station_name} 到 ${end_station_name} 的路径。`};
    }
    
    return {
      success: true,
      start: start_station_name,
      end: end_station_name,
      bike_count: bike_count,
      distance_km: result.distance,
      duration_minutes: result.duration
    };
  }

  private async execute_compare_station_traffic(station_a_name: string, station_b_name: string): Promise<any> {
    try {
      const endDate = new Date().toISOString().split('T')[0];
      const [dataA, dataB] = await Promise.all([
        trafficService.getWeeklySubwayTotals(station_a_name, endDate),
        trafficService.getWeeklySubwayTotals(station_b_name, endDate)
      ]);
      
      return {
        success: true,
        station_a: { name: station_a_name, weekly_data: dataA },
        station_b: { name: station_b_name, weekly_data: dataB }
      };
    } catch (error) {
      console.error('获取周流量数据失败:', error);
      return { error: '获取站点流量数据时发生错误。' };
    }
  }


  /**
   * 发送聊天消息 (可以保留用于不涉及工具调用的纯聊天)
   * @param messages 消息历史
   * @returns 助手回复
   */
  async sendChatMessage(messages: ChatMessage[]): Promise<ChatCompletionResponse> {
    try {
      const response = await axios.post(
        `${this.baseUrl}/chat/completions`,
        {
          model: 'deepseek-chat',
          messages: [
            {
              role: 'system',
              content: '你是一个智能交通监控与调度平台的助手，名为"智能助手"。你可以帮助用户查询系统数据、执行常见操作和分析趋势。对于系统数据，请使用模拟数据进行回复。你的回答应当专业、简洁且有实用价值。支持对话中的Markdown格式。'
            },
            ...messages
          ],
          temperature: 0.7,
          max_tokens: 2000
        },
        {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${this.apiKey}`
          }
        }
      );
      
      if (response.data && response.data.choices && response.data.choices.length > 0) {
        const assistantMessage = response.data.choices[0].message.content;
        return {
          content: assistantMessage
        };
      } else {
        throw new Error('API返回格式不正确');
      }
    } catch (error) {
      console.error('LLM API调用失败:', error);
      // 在API调用失败的情况下返回错误信息
      return {
        content: '抱歉，我暂时无法连接到服务器。请稍后再试。'
      };
    }
  }

  /**
   * 生成调度方案摘要
   * @param params 调度路径参数
   * @returns 生成的调度方案摘要
   */
  async generateDispatchSummary(params: LLMDispatchSummaryRequest): Promise<LLMDispatchSummaryResponse> {
    try {
      const prompt = this.buildDispatchSummaryPrompt(params);
      
      const response = await axios.post(
        `${this.baseUrl}/chat/completions`,
        {
          model: 'deepseek-chat',
          messages: [
            { role: 'system', content: '你是一个专业的交通调度系统专家，负责为用户生成全面、专业、详细的调度方案摘要。你的回答应当包含专业术语、数据分析和具体建议，以帮助调度人员做出更好的决策。' },
            { role: 'user', content: prompt }
          ],
          temperature: 0.7,
          max_tokens: 1000
        },
        {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${this.apiKey}`
          }
        }
      );

      console.log('DeepSeek API响应:', response.data);

      if (response.data && response.data.choices && response.data.choices.length > 0) {
        const content = response.data.choices[0].message.content;
        return this.parseDispatchSummaryResponse(content);
      } else {
        // 如果API调用失败或返回格式不正确，使用模拟数据
        console.warn('DeepSeek API返回格式不正确，使用模拟数据');
        return this.generateMockSummary(params);
      }
    } catch (error) {
      console.error('LLM API调用失败:', error);
      // 在API调用失败的情况下使用模拟数据
      return this.generateMockSummary(params);
    }
  }

  /**
   * 构建调度方案摘要的提示词
   */
  private buildDispatchSummaryPrompt(params: LLMDispatchSummaryRequest): string {
    const currentTime = params.scheduledTime || new Date().toLocaleString('zh-CN');
    const distanceKm = (params.distance / 1000).toFixed(2);
    
    return `
请根据以下调度路径信息，生成一份全面、专业、详细的调度方案摘要，包含多个专业分析部分。

调度路径信息:
- 起点站: ${params.sourceStation.name}（经度: ${params.sourceStation.position.lng}, 纬度: ${params.sourceStation.position.lat}）
- 起点站状态: ${this.translateStatus(params.sourceStation.status)}
- 终点站: ${params.targetStation.name}（经度: ${params.targetStation.position.lng}, 纬度: ${params.targetStation.position.lat}）
- 终点站状态: ${this.translateStatus(params.targetStation.status)}
- 调度单车数量: ${params.bikeCount} 辆
- 路径总距离: ${distanceKm} 公里
- 预计耗时: ${params.duration} 分钟
- 当前/计划执行时间: ${currentTime}
${params.roadNames ? `- 途经主要道路: ${params.roadNames.join('、')}` : ''}

请按照以下JSON格式输出结果，确保内容专业、详细且有实用价值:
{
  "title": "专业的方案名称，不超过25个字",
  "summary": "一段包含核心要素的专业描述性文本，150-200字，包含调度目的、路线特点和关键考量",
  "riskHint": "基于地理位置、时间和交通状况的潜在风险提醒，80-100字",
  "executionPlan": "详细的执行计划步骤，包括时间节点、人员安排和操作流程，200-250字",
  "trafficAnalysis": "对途经路段的交通状况分析，包括高峰期预测、拥堵点识别和绕行建议，150-200字",
  "resourceRequirements": "完成此次调度所需的人力、车辆和设备资源分析，100-150字",
  "expectedBenefits": "此次调度对系统平衡性和用户体验的预期改善效果分析，100-150字",
  "alternativeRoutes": "1-2条备选路线建议，包括各自的优缺点对比，100-150字"
}

请确保输出是有效的JSON格式，不要添加额外的解释或标记。所有内容应当专业、具体、有针对性，避免空泛的表述。
`;
  }

  /**
   * 解析LLM返回的调度方案摘要
   */
  private parseDispatchSummaryResponse(content: string): LLMDispatchSummaryResponse {
    try {
      // 尝试提取JSON部分
      const jsonMatch = content.match(/\{[\s\S]*\}/);
      if (jsonMatch) {
        const jsonStr = jsonMatch[0];
        const result = JSON.parse(jsonStr);
        
        // 验证返回的JSON是否包含所需字段
        if (!result.title || !result.summary || !result.riskHint) {
          throw new Error('响应缺少必要字段');
        }
        
        return {
          title: result.title,
          summary: result.summary,
          riskHint: result.riskHint,
          executionPlan: result.executionPlan,
          trafficAnalysis: result.trafficAnalysis,
          resourceRequirements: result.resourceRequirements,
          expectedBenefits: result.expectedBenefits,
          alternativeRoutes: result.alternativeRoutes
        };
      } else {
        // 如果无法提取JSON，则进行简单的文本解析
        const lines = content.split('\n').filter(line => line.trim());
        return {
          title: lines[0]?.replace(/^标题[：:]\s*/, '') || '调度方案',
          summary: lines[1]?.replace(/^摘要[：:]\s*/, '') || '调度方案摘要',
          riskHint: lines[2]?.replace(/^风险提示[：:]\s*/, '') || '请注意路途中可能的交通状况'
        };
      }
    } catch (error) {
      console.error('解析LLM响应失败:', error, content);
      return {
        title: '调度方案',
        summary: '从起点站调度单车到终点站',
        riskHint: '请注意路途中可能的交通状况'
      };
    }
  }

  /**
   * 生成模拟的调度方案摘要（当API调用失败时使用）
   */
  private generateMockSummary(params: LLMDispatchSummaryRequest): LLMDispatchSummaryResponse {
    const timeStr = params.scheduledTime ? 
      new Date(params.scheduledTime).toLocaleTimeString('zh-CN', {hour: '2-digit', minute:'2-digit'}) : 
      '当前时间';
    
    const distanceKm = parseFloat((params.distance / 1000).toFixed(1));
    const durationMin = Math.round(params.duration / 60);
    const bikeCount = params.bikeCount;
    
    return {
      title: `${timeStr}：${params.sourceStation.name}至${params.targetStation.name}调度方案`,
      summary: `本次任务计划从单车${this.translateStatus(params.sourceStation.status)}的【${params.sourceStation.name}】站向单车${this.translateStatus(params.targetStation.status)}的【${params.targetStation.name}】站调配${bikeCount}辆单车，距离${distanceKm}公里，预计耗时${durationMin}分钟。此次调度旨在平衡站点间的单车分布，提高系统整体运行效率和用户满意度。`,
      riskHint: `请注意途经路段可能存在交通拥堵，建议调度员提前规划路线并预留充足时间。特别关注高峰期交通状况，可能需要调整出发时间以避开拥堵路段。`,
      executionPlan: `建议调度团队提前15分钟到达起点站，确认单车状态并完成装载。调度过程中保持与调度中心的通信联系，每经过关键路段时报告进度。预计需要2名调度员协同完成，一人负责驾驶，一人负责导航和通信。`,
      trafficAnalysis: `根据历史数据分析，该路线在工作日${timeStr.includes('上午') ? '早高峰(7:30-9:00)' : '晚高峰(17:30-19:00)'}期间可能遇到中度拥堵，特别是在主要交叉路口。建议提前规划备选路线，必要时可通过小路绕行以节省时间。`,
      resourceRequirements: `本次调度建议使用中型货车，可一次性装载${bikeCount}辆单车。需配备2名调度人员，携带基础维修工具和安全装备。预计燃油消耗约${(distanceKm * 0.1).toFixed(1)}升，总调度成本约${Math.round(distanceKm * 2 + bikeCount * 5)}元。`,
      expectedBenefits: `完成此次调度后，预计将使${params.sourceStation.name}站的单车饱和度降低约${Math.min(100, Math.round(bikeCount * 5))}%，同时提高${params.targetStation.name}站的单车可用率约${Math.min(100, Math.round(bikeCount * 8))}%。这将显著改善用户体验，预计可减少用户等待时间约3-5分钟。`,
      alternativeRoutes: `备选路线1：可考虑经由次干道行驶，虽然距离增加约0.8公里，但高峰期可避开主要拥堵点，总体时间可能缩短5-8分钟。备选路线2：如遇特殊情况，可选择绕行环城路，虽然距离增加1.5公里，但道路宽阔，适合大型车辆通行。`
    };
  }

  /**
   * 将状态英文转换为中文描述
   */
  private translateStatus(status: string): string {
    switch (status) {
      case 'surplus': return '单车富余';
      case 'shortage': return '单车短缺';
      case 'balanced': return '供需平衡';
      default: return status;
    }
  }
}

// 导出单例实例
export const llmService = new LLMService('sk-82e2bb3eb9c54c7494cc3689285e0019'); 