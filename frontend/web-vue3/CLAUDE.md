# web-vue3 CLAUDE.md

## 项目概述
HavenButler Web管理端是基于Vue3的现代化智能家居管理平台前端，严格遵循Google Material Design 3设计规范，提供直观、流畅的用户体验。

## 设计理念

### 1. Material Design 3 核心原则
- **动态颜色**：支持从用户壁纸提取主题色
- **圆角设计**：使用大圆角营造柔和感
- **表面层级**：通过色调变化区分层级
- **触感反馈**：Ripple效果和状态变化
- **自适应布局**：响应式栅格系统

### 2. 组件库选择
```javascript
/**
 * 主要UI框架：Vuetify 3
 * 原因：原生支持Material Design 3
 */
import { createVuetify } from 'vuetify'
import { md3 } from 'vuetify/blueprints'

const vuetify = createVuetify({
  blueprint: md3,
  theme: {
    defaultTheme: 'light',
    themes: {
      light: {
        colors: {
          primary: '#6750A4',
          secondary: '#625B71',
          tertiary: '#7D5260',
        }
      }
    }
  }
})
```

## UI组件参考资源

### 1. 基础组件库
- **Vuetify 3**：https://vuetifyjs.com/
- **Element Plus**：https://element-plus.org/
- **Naive UI**：https://www.naiveui.com/
- **Ant Design Vue**：https://antdv.com/

### 2. 特色组件资源
- **UIVerse组件**：https://uiverse.io/elements
  - 创意按钮效果
  - 加载动画
  - 卡片设计
  - 输入框效果

### 3. 图表组件
- **ECharts**：https://echarts.apache.org/
- **Chart.js**：https://www.chartjs.org/
- **D3.js**：https://d3js.org/
- **ApexCharts**：https://apexcharts.com/

### 4. 动画库
- **Lottie**：https://lottiefiles.com/
- **Framer Motion**：https://www.framer.com/motion/
- **AutoAnimate**：https://auto-animate.formkit.com/
- **Vue Transitions**：https://vuejs.org/guide/built-ins/transition.html

## 开发最佳实践

### 1. 组件设计模式

#### 1.1 智能组件 vs 展示组件
```vue
<!-- 智能组件：DeviceController.vue -->
<template>
  <DeviceCard 
    :device="device"
    :loading="loading"
    @toggle="handleToggle"
  />
</template>

<script setup>
// 处理业务逻辑
const device = computed(() => store.device)
const handleToggle = async () => {
  await api.toggleDevice(device.id)
}
</script>

<!-- 展示组件：DeviceCard.vue -->
<template>
  <md-card>
    <!-- 纯展示，通过props和events通信 -->
  </md-card>
</template>
```

#### 1.2 组合式函数复用
```typescript
// composables/useDevice.ts
export function useDevice(deviceId: string) {
  const device = ref<Device>()
  const loading = ref(false)
  
  const fetchDevice = async () => {
    loading.value = true
    try {
      device.value = await api.getDevice(deviceId)
    } finally {
      loading.value = false
    }
  }
  
  const toggleDevice = async () => {
    await api.toggleDevice(deviceId)
    await fetchDevice()
  }
  
  onMounted(fetchDevice)
  
  return {
    device: readonly(device),
    loading: readonly(loading),
    toggleDevice
  }
}
```

### 2. Material Design 3 组件实现

#### 2.1 动态颜色系统
```scss
// styles/theme.scss
:root {
  // Primary colors
  --md-sys-color-primary: #6750A4;
  --md-sys-color-on-primary: #FFFFFF;
  --md-sys-color-primary-container: #EADDFF;
  --md-sys-color-on-primary-container: #21005D;
  
  // Surface colors
  --md-sys-color-surface: #FFFBFE;
  --md-sys-color-on-surface: #1C1B1F;
  --md-sys-color-surface-variant: #E7E0EC;
  --md-sys-color-on-surface-variant: #49454F;
  
  // Elevation
  --md-sys-elevation-1: 0 1px 2px rgba(0,0,0,0.3);
  --md-sys-elevation-2: 0 2px 6px rgba(0,0,0,0.3);
  --md-sys-elevation-3: 0 4px 8px rgba(0,0,0,0.3);
  
  // Shape
  --md-sys-shape-corner-small: 8px;
  --md-sys-shape-corner-medium: 12px;
  --md-sys-shape-corner-large: 16px;
  --md-sys-shape-corner-extra-large: 28px;
}
```

#### 2.2 Material You卡片组件
```vue
<template>
  <div class="md-card" :class="elevationClass">
    <div class="md-card__surface">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.md-card {
  border-radius: var(--md-sys-shape-corner-large);
  overflow: hidden;
  transition: all 0.2s ease;
}

.md-card__surface {
  background: var(--md-sys-color-surface);
  color: var(--md-sys-color-on-surface);
  padding: 16px;
}

.md-card--elevated-1 {
  box-shadow: var(--md-sys-elevation-1);
}

.md-card:hover {
  box-shadow: var(--md-sys-elevation-2);
}
</style>
```

### 3. 实时数据更新

#### 3.1 WebSocket连接管理
```typescript
// composables/useWebSocket.ts
export function useWebSocket() {
  const socket = ref<Socket>()
  const connected = ref(false)
  
  const connect = () => {
    socket.value = io(WS_URL, {
      auth: {
        token: getToken()
      }
    })
    
    socket.value.on('connect', () => {
      connected.value = true
      console.log('WebSocket connected')
    })
    
    socket.value.on('device:update', (data) => {
      // 更新设备状态
      deviceStore.updateDevice(data)
    })
    
    socket.value.on('notification', (data) => {
      // 显示通知
      showNotification(data)
    })
  }
  
  const disconnect = () => {
    socket.value?.disconnect()
    connected.value = false
  }
  
  onMounted(connect)
  onUnmounted(disconnect)
  
  return {
    socket: readonly(socket),
    connected: readonly(connected)
  }
}
```

### 4. 性能优化策略

#### 4.1 虚拟列表实现
```vue
<template>
  <VirtualList
    :items="devices"
    :item-height="80"
    :buffer="5"
  >
    <template #default="{ item }">
      <DeviceItem :device="item" />
    </template>
  </VirtualList>
</template>
```

#### 4.2 图片懒加载
```vue
<template>
  <img v-lazy="imageSrc" :alt="altText">
</template>

<script setup>
// 使用vue-lazyload插件
import VueLazyload from 'vue-lazyload'

app.use(VueLazyload, {
  preLoad: 1.3,
  error: '/error.png',
  loading: '/loading.gif',
  attempt: 1
})
</script>
```

### 5. 响应式设计

#### 5.1 断点系统
```scss
// styles/breakpoints.scss
$breakpoints: (
  'xs': 0,      // 手机竖屏
  'sm': 600px,  // 手机横屏
  'md': 960px,  // 平板
  'lg': 1280px, // 笔记本
  'xl': 1920px  // 桌面
);

@mixin respond-to($breakpoint) {
  @media (min-width: map-get($breakpoints, $breakpoint)) {
    @content;
  }
}

// 使用示例
.container {
  padding: 16px;
  
  @include respond-to('md') {
    padding: 24px;
  }
  
  @include respond-to('lg') {
    padding: 32px;
  }
}
```

### 6. 错误处理

#### 6.1 全局错误捕获
```typescript
// main.ts
app.config.errorHandler = (err, instance, info) => {
  console.error('Global error:', err)
  
  // 发送错误到监控服务
  if (import.meta.env.PROD) {
    reportError({
      error: err,
      info: info,
      url: window.location.href,
      userAgent: navigator.userAgent
    })
  }
  
  // 显示用户友好的错误提示
  showErrorToast('操作失败，请稍后重试')
}
```

### 7. 国际化支持

```typescript
// locales/zh-CN.ts
export default {
  common: {
    confirm: '确认',
    cancel: '取消',
    save: '保存',
    delete: '删除'
  },
  device: {
    title: '设备管理',
    online: '在线',
    offline: '离线',
    control: '控制'
  }
}

// 使用
const { t } = useI18n()
console.log(t('device.title'))
```

## 开发工具推荐

### 1. VSCode插件
- **Vue Language Features**：Vue3语法支持
- **TypeScript Vue Plugin**：TS支持
- **Vuetify Vscode**：Vuetify组件提示
- **Material Icon Theme**：Material图标
- **Error Lens**：错误高亮

### 2. Chrome扩展
- **Vue.js devtools**：Vue调试工具
- **Redux DevTools**：状态管理调试
- **Lighthouse**：性能分析
- **ColorZilla**：颜色提取

### 3. 设计工具
- **Figma**：UI设计
- **Material Theme Builder**：主题生成
- **Coolors**：配色方案
- **Iconfont**：图标资源

## 测试策略

### 1. 单元测试
```typescript
// DeviceCard.spec.ts
import { mount } from '@vue/test-utils'
import DeviceCard from '@/components/DeviceCard.vue'

describe('DeviceCard', () => {
  it('renders device name', () => {
    const wrapper = mount(DeviceCard, {
      props: {
        device: {
          name: 'Living Room Light',
          status: 'on'
        }
      }
    })
    
    expect(wrapper.text()).toContain('Living Room Light')
  })
})
```

### 2. E2E测试
```typescript
// e2e/device.spec.ts
import { test, expect } from '@playwright/test'

test('toggle device', async ({ page }) => {
  await page.goto('/device')
  await page.click('[data-test="device-toggle"]')
  await expect(page.locator('[data-test="device-status"]'))
    .toHaveText('off')
})
```

## 部署注意事项

### 1. 环境变量
```bash
# .env.production
VITE_API_BASE_URL=https://api.havenbutler.com
VITE_WS_URL=wss://ws.havenbutler.com
VITE_ENABLE_MOCK=false
```

### 2. 构建优化
```javascript
// vite.config.ts
export default {
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor': ['vue', 'pinia', 'vue-router'],
          'ui': ['vuetify'],
          'charts': ['echarts']
        }
      }
    },
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true
      }
    }
  }
}
```

## 常见问题

### Q: 如何实现暗黑模式？
A: 使用Vuetify的内置主题系统，配合CSS变量动态切换

### Q: 如何优化首屏加载？
A: 路由懒加载、组件异步加载、资源预加载、CDN加速

### Q: 如何处理大量设备列表？
A: 使用虚拟滚动、分页加载、搜索过滤

### Q: 如何保证UI一致性？
A: 严格遵循Material Design 3规范，使用设计令牌系统