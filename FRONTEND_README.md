# 地铁智慧监控与调度平台 - 前端说明文档

本文档旨在为 “地铁智慧监控与调度平台” 的前端项目提供全面的技术和结构说明，帮助开发者快速理解项目并进行后续的开发与维护。

## 1. 技术栈概览

本项目是一个基于 Vue 3 构建的现代化单页应用 (SPA)，采用了业界主流的技术方案：

-   **核心框架**: [Vue.js](https://cn.vuejs.org/) `v3.3`
-   **构建工具**: [Vite](https://cn.vitejs.dev/) `v5.0`
-   **编程语言**: [TypeScript](https://www.typescriptlang.org/) `v5.3`
-   **路由管理**: [Vue Router](https://router.vuejs.org/zh/) `v4.2`
-   **状态管理**: [Pinia](https://pinia.vuejs.org/zh/) `v2.1`
-   **UI 组件库**: [Element Plus](https://element-plus.org/zh-CN/) `v2.10`
-   **HTTP客户端**: [Axios](https://axios-http.com/) `v1.10`
-   **数据可视化**: [Apache ECharts](https://echarts.apache.org/zh/index.html) `v5.4`
-   **CSS预处理器**: [Sass/SCSS](https://sass-lang.com/)

## 2. 项目结构

项目源代码位于根目录下的 `src/` 文件夹中，遵循了 Vue 社区的最佳实践进行组织。

```
src/
├── api/          # API 请求模块 (封装 Axios)
├── assets/       # 静态资源 (图片、字体、全局样式)
├── components/   # 可复用的小组件 (如 ECharts 图表封装)
├── config/       # 项目配置文件 (如 API 基础路径)
├── router/       # 路由配置 (index.ts)
├── services/     # (备用) 更复杂的业务服务封装
├── stores/       # Pinia 状态管理模块 (如 userStore.ts)
├── types/        # TypeScript 类型定义
├── views/        # 页面级组件
│   ├── DataAnalysis.vue  # 核心：数据分析页
│   ├── Dashboard.vue     # 仪表盘
│   ├── Login.vue         # 登录页
│   └── ...               # 其他页面
├── App.vue       # 根组件
└── main.ts       # 应用入口文件
```

### 关键目录说明

-   **`views/`**: 存放与路由直接对应的“页面”组件。每个 `.vue` 文件代表一个独立的功能页面。
-   **`components/`**: 存放被 `views/` 或其他组件复用的“子组件”。例如，一个封装了 ECharts 的图表组件可以放在这里，被数据分析页多次调用以展示不同类型的图表。
-   **`router/index.ts`**: 定义了应用的页面路由和权限控制。通过路由守卫（`beforeEach`）实现未登录跳转和基于角色的页面访问控制。
-   **`stores/`**: 使用 Pinia 管理全局状态。例如，`userStore.ts` 用于存储登录后用户的 token、角色等信息，供整个应用使用。
-   **`api/`**: 负责与后端进行数据交互。通常会在这里封装 Axios，定义各个后端接口的调用函数，便于管理和复用。

## 3. 核心功能解析

### 3.1 权限与登录流程

1.  用户访问应用，默认进入登录页 (`/login`)。
2.  输入凭据后，前端调用后端登录接口。成功后，后端返回一个 **JWT Token**。
3.  前端将此 Token 存入浏览器的 `localStorage`，并存储用户信息（如角色）到 Pinia 的 `userStore` 中。
4.  页面跳转到仪表盘 (`/`) 或其他目标页面。
5.  在后续的每次路由跳转时，`router/index.ts` 中的路由守卫会执行：
    -   检查 `localStorage` 中是否存在 Token，若无，则强制跳转回登录页。
    -   检查 `userStore` 中的用户角色是否满足目标页面的权限要求，若不满足，则提示并阻止访问。
6.  在每次调用后端业务接口时，HTTP 请求头中会附带此 Token，由后端进行身份验证。

### 3.2 数据分析页面 (`views/DataAnalysis.vue`)

这是展示地铁客流量数据的核心页面。其工作流程如下：

1.  **初始化**: 页面加载时，会触发 `onMounted` 生命周期钩子。
2.  **API 调用**: 在钩子函数中，页面会调用 `api/` 目录下封装好的函数，向后端 `/api/subway/...` 的各个端点发起请求。例如：
    -   调用 `getDateAmount(date)` 来请求 `/api/subway/date` 接口，获取指定日期的各站流量。
    -   调用 `getWeeklyTotals(station, endDate)` 来请求 `/api/subway/weekly` 接口，获取某站近7日流量。
    -   其他接口（如 `/totals`, `/map`, `/trend`）同理。
3.  **数据处理**: 获取到后端返回的数据后，页面可能会对数据进行一些转换，使其符合 ECharts 图表的格式要求。
4.  **图表渲染**: 将处理好的数据传递给封装在 `components/` 目录下的 ECharts 组件，由该组件负责渲染出线图、柱状图、地图等各种可视化图表。
5.  **用户交互**: 页面上会提供日期选择器、站点选择框等控件。当用户改变这些输入时，会触发新的 API 请求，并更新图表数据，实现动态查询。

## 4. 如何进行二次开发

### 示例：添加一个新的图表

假设您需要在一个新页面中添加一个“全站点月度流量总览”的图表。

1.  **后端开发**: 首先，需要后端同学在 `SubAmountController` 中添加一个新的接口，例如 `GET /api/subway/monthly`，并实现相应的 Service 和 Mapper 逻辑。
2.  **前端 - API 层**: 在 `src/api/` 目录下，添加一个新的导出函数：
    ```typescript
    // src/api/subway.ts (假设)
    export function getMonthlyData(month) {
      return axios.get(`/api/subway/monthly`, { params: { month } });
    }
    ```
3.  **前端 - 页面层**:
    -   在 `src/views/` 下创建一个新的页面组件，例如 `MonthlyReport.vue`。
    -   在 `src/router/index.ts` 中为这个新页面添加路由配置，并设置好标题和所需权限。
4.  **前端 - 组件调用**:
    -   在 `MonthlyReport.vue` 中，引入 `getMonthlyData` 函数。
    -   在 `onMounted` 钩子或某个按钮的点击事件中调用 `getMonthlyData`。
    -   获取数据后，将其传递给一个 ECharts 图表组件进行渲染。如果现有图表组件不满足需求，可以在 `src/components/` 下创建一个新的可复用图表组件。

### 示例：修改现有页面的 UI

如果您想调整“数据分析”页面的布局或样式：

1.  直接打开 `src/views/DataAnalysis.vue` 文件。
2.  修改 `<template>` 部分的 HTML 结构，或调整 `<style>` 部分的 SCSS 样式。
3.  由于项目使用了 Element Plus，您可以查阅其官方文档，使用或修改其组件来实现您想要的 UI 效果。

---

希望这份文档能帮助您更好地理解和维护此项目。 