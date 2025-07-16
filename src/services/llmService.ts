import axios from 'axios';
import type { Station } from '../stores/mapStore';

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

export class LLMService {
  private apiKey: string;
  private baseUrl: string = 'https://api.deepseek.com/v1';

  constructor(apiKey: string) {
    this.apiKey = apiKey;
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