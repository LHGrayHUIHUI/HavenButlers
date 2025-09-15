# HavenButler 前端参考资源库

## 核心组件库参考

### 1. UIVerse.io 组件库

官网：https://uiverse.io/

#### 使用说明
UIVerse.io 是一个开源CSS/HTML组件库，提供各种创意UI组件。由于组件更新频繁，建议：
1. 访问主站搜索所需组件类型
2. 根据实际需求选择合适的组件
3. 复制代码并转换为Vue3组件格式

#### 推荐搜索关键词
- **按钮效果**：`button`、`switch`、`toggle`
  - 适用场景：设备开关控制、场景激活
  - 搜索建议：`glow button`（发光按钮）、`ripple button`（波纹按钮）
  
- **卡片设计**：`card`、`flip`、`glass`
  - 适用场景：设备状态展示、数据统计
  - 搜索建议：`3d card`（3D卡片）、`glassmorphism`（玻璃态）

- **加载动画**：`loader`、`spinner`、`progress`
  - 适用场景：设备连接、数据同步
  - 搜索建议：`circle loader`（环形加载）、`progress bar`（进度条）

- **输入组件**：`input`、`form`、`text field`
  - 适用场景：表单输入、搜索框
  - 搜索建议：`floating label`（浮动标签）、`material input`（Material风格）

- **开关组件**：`toggle`、`switch`、`checkbox`
  - 适用场景：设备开关、功能切换
  - 搜索建议：`ios switch`（iOS风格）、`animated toggle`（动画开关）

- **通知组件**：`toast`、`notification`、`alert`
  - 适用场景：操作反馈、系统通知
  - 搜索建议：`toast message`、`popup notification`

### 2. 稳定的CSS组件库推荐

#### CSS框架和组件
- **Tailwind UI**：https://tailwindui.com/components
  - 专业的组件模板库（付费）
  - 提供完整的Vue3集成示例

- **Headless UI**：https://headlessui.com/
  - 无样式的交互组件
  - 完全支持Vue3，可自定义样式

- **DaisyUI**：https://daisyui.com/
  - 基于Tailwind CSS的组件库
  - 开源免费，主题丰富

- **Flowbite**：https://flowbite.com/
  - Tailwind CSS组件库
  - 提供Vue3集成指南

#### Material Design组件
- **Material Design 官方组件**：https://github.com/material-components/material-web
  - Google官方Web Components
  - 可在Vue3中使用

- **Material Design Icons**：https://materialdesignicons.com/
  - 完整的MD图标库
  - 支持Vue3组件形式使用

## Vue3 生态推荐

### UI框架

#### 1. Vuetify 3 (首选)
- 官网：https://vuetifyjs.com/
- 优势：原生支持Material Design 3
- 使用场景：作为主UI框架
- 组件覆盖度：95%
- 社区活跃度：⭐⭐⭐⭐⭐

#### 2. Quasar Framework
- 官网：https://quasar.dev/
- 优势：一套代码多端运行（Web/Mobile/Desktop）
- Material Design支持：优秀
- 适合：需要快速开发多平台应用

#### 3. PrimeVue
- 官网：https://primevue.org/
- 优势：丰富的企业级组件
- 主题系统：Material Design主题可选
- 特色：高级数据表格、图表组件

#### 4. Naive UI
- 官网：https://www.naiveui.com/
- 优势：TypeScript友好、树摇优化好
- 设计风格：现代简约
- 性能：优秀

### 图表可视化

#### 1. Apache ECharts
- 官网：https://echarts.apache.org/
- 推荐指数：⭐⭐⭐⭐⭐
- 使用场景：
  - 能耗分析图表
  - 设备使用统计
  - 实时数据监控
- Vue3集成：vue-echarts

#### 2. Chart.js
- 官网：https://www.chartjs.org/
- 推荐指数：⭐⭐⭐⭐
- 优势：轻量级、易上手
- Vue3集成：vue-chartjs

#### 3. D3.js
- 官网：https://d3js.org/
- 推荐指数：⭐⭐⭐⭐
- 使用场景：自定义可视化、设备拓扑图
- 学习曲线：较陡峭

#### 4. AntV G2
- 官网：https://g2.antv.vision/
- 推荐指数：⭐⭐⭐⭐
- 特色：语法简洁、交互丰富
- 适合：数据分析场景

### 动画库

#### 1. Lottie
- 官网：https://airbnb.io/lottie/
- 使用场景：
  - 设备状态动画
  - 加载动画
  - 空状态插画
- Vue3集成：vue3-lottie

#### 2. GreenSock (GSAP)
- 官网：https://greensock.com/
- 使用场景：复杂动画序列
- 性能：极佳
- 推荐版本：GSAP 3

#### 3. AutoAnimate
- 官网：https://auto-animate.formkit.com/
- 优势：零配置、自动化
- 使用场景：列表排序、元素增删动画
- Vue3支持：原生支持

#### 4. Motion One
- 官网：https://motion.dev/
- 优势：轻量级（3KB）
- API：简洁现代
- 性能：优秀

### 实用工具库

#### 1. VueUse
- 官网：https://vueuse.org/
- 描述：Vue3组合式API工具集合
- 推荐功能：
  - useWebSocket - WebSocket连接管理
  - useStorage - 本地存储
  - useThrottleFn - 函数节流
  - useIntersectionObserver - 懒加载

#### 2. Pinia
- 官网：https://pinia.vuejs.org/
- 用途：状态管理（Vuex替代品）
- 优势：TypeScript支持好、API简洁
- 必需程度：核心依赖

#### 3. Tanstack Query
- 官网：https://tanstack.com/query/
- 用途：服务端状态管理
- 特色：缓存、同步、后台更新
- 适合：复杂数据获取场景

#### 4. Day.js
- 官网：https://day.js.org/
- 用途：日期时间处理
- 优势：轻量级Moment.js替代品
- 大小：2KB

### 开发工具

#### 1. Vue DevTools
- 下载：Chrome/Firefox扩展商店
- 版本：Vue DevTools 6 (支持Vue3)
- 功能：组件树、状态、性能分析

#### 2. Vite
- 官网：https://vitejs.dev/
- 用途：构建工具
- 优势：快速冷启动、HMR
- 已集成：项目默认使用

#### 3. Histoire
- 官网：https://histoire.dev/
- 用途：组件开发和文档
- 类似：Storybook的Vue3替代品
- 优势：更快、更轻量

#### 4. Vitest
- 官网：https://vitest.dev/
- 用途：单元测试
- 优势：与Vite完美集成
- API：兼容Jest

## 智能家居UI参考项目

### 开源项目

#### 1. Home Assistant Frontend
- GitHub：https://github.com/home-assistant/frontend
- 技术栈：Lit + TypeScript
- 可借鉴：
  - 设备卡片设计
  - 仪表盘布局
  - 实时数据更新机制

#### 2. ioBroker.vis
- GitHub：https://github.com/ioBroker/ioBroker.vis-2
- 技术栈：React + Material-UI  
- 可借鉴：
  - 可视化编辑器
  - 拖拽式界面构建
  - 组件库设计

#### 3. OpenHAB UI
- GitHub：https://github.com/openhab/openhab-webui
- 技术栈：Vue 3 + Framework7
- 可借鉴：
  - 规则引擎UI
  - 设备自动发现界面
  - 主题系统

#### 4. Gladys Assistant
- GitHub：https://github.com/GladysAssistant/Gladys
- 技术栈：Preact
- 可借鉴：
  - 场景管理界面
  - 多语言支持
  - 仪表盘组件

### 商业产品UI参考

#### 1. 小米米家
- 特点：简洁、卡片式布局
- 可借鉴：
  - 设备分组方式
  - 快捷操作设计
  - 场景联动展示

#### 2. Apple HomeKit
- 特点：优雅、注重细节
- 可借鉴：
  - 房间视图设计
  - 设备控制交互
  - 自动化规则UI

#### 3. Google Home
- 特点：Material Design典范
- 可借鉴：
  - 设备类型图标
  - 语音交互界面
  - 快速操作面板

#### 4. Amazon Alexa
- 特点：功能丰富、模块化
- 可借鉴：
  - 技能市场设计
  - 例行程序界面
  - 设备分组管理

## Material Design 3 资源

### 官方资源
- **设计规范**：https://m3.material.io/
- **图标库**：https://fonts.google.com/icons
- **颜色工具**：https://m3.material.io/theme-builder
- **设计套件**：https://www.figma.com/community/file/1035203688168086460

### 社区资源
- **Material Design 3 for Vue**：https://github.com/material-components/material-web
- **MD3 主题生成器**：https://material-foundation.github.io/material-theme-builder/
- **动效示例**：https://material.io/design/motion/

### 设计工具
- **Figma MD3 UI Kit**：完整组件库
- **Adobe XD MD3 Kit**：Adobe生态集成
- **Sketch MD3 Library**：Mac设计工具

## 性能优化资源

### 1. 图片优化
- **Sharp**：Node.js图片处理
- **WebP转换**：现代图片格式
- **Lazy Loading**：vue-lazyload
- **Progressive Image**：渐进式加载

### 2. 打包优化
- **Rollup Visualizer**：包体积分析
- **Vite Compression**：Gzip/Brotli压缩
- **Tree Shaking**：死代码消除
- **Code Splitting**：代码分割

### 3. 运行时优化  
- **Virtual Scroller**：虚拟滚动
- **Web Workers**：计算密集型任务
- **Service Worker**：离线缓存
- **Memory Management**：内存泄漏检测

## 测试资源

### 单元测试
- **Vitest**：推荐，Vite原生支持
- **Jest**：成熟方案
- **Vue Test Utils**：官方测试工具

### E2E测试
- **Playwright**：推荐，跨浏览器
- **Cypress**：开发体验好
- **Nightwatch**：Vue官方推荐

### 视觉回归测试
- **Percy**：云端视觉测试
- **BackstopJS**：开源方案
- **Chromatic**：Storybook集成

## 部署和监控

### 部署平台
- **Vercel**：最简单的部署方案
- **Netlify**：强大的CI/CD
- **Cloudflare Pages**：全球CDN
- **自建Docker**：完全控制

### 监控工具
- **Sentry**：错误追踪
- **LogRocket**：会话回放
- **Google Analytics**：用户分析
- **Grafana**：性能监控

## 学习资源

### 官方文档
- Vue 3文档：https://vuejs.org/
- Vite文档：https://vitejs.dev/
- TypeScript手册：https://www.typescriptlang.org/docs/

### 视频教程
- Vue Mastery：https://www.vuemastery.com/
- Vue School：https://vueschool.io/
- YouTube - Traversy Media
- YouTube - The Net Ninja

### 社区
- Vue.js中文社区：https://cn.vuejs.org/
- Vue Forum：https://forum.vuejs.org/
- Discord：Vue Land
- Reddit：r/vuejs

## 项目集成建议

### 第一阶段（核心功能）
1. Vuetify 3 - 主UI框架
2. Pinia - 状态管理
3. Vue Router - 路由管理
4. VueUse - 工具函数
5. ECharts - 数据可视化

### 第二阶段（增强体验）
1. UIVerse组件 - 特色交互
2. Lottie - 动画效果
3. AutoAnimate - 列表动画
4. Virtual Scroller - 性能优化

### 第三阶段（完善功能）
1. PWA支持 - 离线功能
2. i18n - 国际化
3. 主题切换 - 暗黑模式
4. 无障碍 - ARIA支持

## 更新记录
- 2024-01-15: 初始版本，整理核心资源
- 待更新: 持续收集优质资源