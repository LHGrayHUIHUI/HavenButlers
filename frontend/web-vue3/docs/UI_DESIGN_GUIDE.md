# HavenButler UI设计规范 (Material Design 3)

## 设计理念

### 核心原则
1. **表达性**：通过动态颜色和个性化主题表达品牌特色
2. **实用性**：清晰的层级结构和直观的交互
3. **愉悦性**：流畅的动画和令人愉悦的视觉反馈
4. **适应性**：响应式设计，适配各种设备

## 颜色系统

### 动态颜色方案
```scss
// 主色调
$md-ref-palette-primary: (
  0: #000000,
  10: #21005D,
  20: #381E72,
  30: #4F378B,
  40: #6750A4,    // Primary
  50: #7F67BE,
  60: #9A82DB,
  70: #B69DF8,
  80: #D0BCFF,    // Primary Container
  90: #EADDFF,
  95: #F6EDFF,
  99: #FFFBFE,
  100: #FFFFFF
);

// 次要色调
$md-ref-palette-secondary: (
  40: #625B71,    // Secondary
  80: #E8DEF8     // Secondary Container
);

// 第三色调
$md-ref-palette-tertiary: (
  40: #7D5260,    // Tertiary
  80: #FFD8E4     // Tertiary Container
);

// 错误色
$md-ref-palette-error: (
  40: #BA1A1A,    // Error
  80: #FFB4AB     // Error Container
);
```

### 表面颜色层级
```scss
// 亮色主题
$light-theme: (
  surface: #FFFBFE,
  surface-dim: #DED8E1,
  surface-bright: #FFFBFE,
  surface-container-lowest: #FFFFFF,
  surface-container-low: #F7F2FA,
  surface-container: #F3EDF7,
  surface-container-high: #ECE6F0,
  surface-container-highest: #E6E0E9
);

// 暗色主题
$dark-theme: (
  surface: #141218,
  surface-dim: #141218,
  surface-bright: #3B383E,
  surface-container-lowest: #0F0D13,
  surface-container-low: #1D1B20,
  surface-container: #211F26,
  surface-container-high: #2B2930,
  surface-container-highest: #36343B
);
```

## 排版系统

### 字体规范
```scss
// 字体家族
$font-family-base: 'Roboto', 'Noto Sans SC', sans-serif;
$font-family-mono: 'Roboto Mono', monospace;

// 字体大小
$typography-scale: (
  display-large: (size: 57px, line-height: 64px, weight: 400),
  display-medium: (size: 45px, line-height: 52px, weight: 400),
  display-small: (size: 36px, line-height: 44px, weight: 400),
  
  headline-large: (size: 32px, line-height: 40px, weight: 400),
  headline-medium: (size: 28px, line-height: 36px, weight: 400),
  headline-small: (size: 24px, line-height: 32px, weight: 400),
  
  title-large: (size: 22px, line-height: 28px, weight: 400),
  title-medium: (size: 16px, line-height: 24px, weight: 500),
  title-small: (size: 14px, line-height: 20px, weight: 500),
  
  body-large: (size: 16px, line-height: 24px, weight: 400),
  body-medium: (size: 14px, line-height: 20px, weight: 400),
  body-small: (size: 12px, line-height: 16px, weight: 400),
  
  label-large: (size: 14px, line-height: 20px, weight: 500),
  label-medium: (size: 12px, line-height: 16px, weight: 500),
  label-small: (size: 11px, line-height: 16px, weight: 500)
);
```

## 形状系统

### 圆角规范
```scss
$shape-scale: (
  none: 0,
  extra-small: 4px,
  small: 8px,
  medium: 12px,
  large: 16px,
  extra-large: 28px,
  full: 50%
);

// 组件圆角应用
.card { border-radius: $shape-large; }
.button { border-radius: $shape-full; }
.dialog { border-radius: $shape-extra-large; }
.chip { border-radius: $shape-small; }
.input { border-radius: $shape-extra-small; }
```

## 高度系统

### 投影层级
```scss
$elevation-scale: (
  level0: none,
  level1: (
    umbra: 0px 1px 3px 0px rgba(0, 0, 0, 0.2),
    penumbra: 0px 1px 1px 0px rgba(0, 0, 0, 0.14),
    ambient: 0px 2px 1px -1px rgba(0, 0, 0, 0.12)
  ),
  level2: (
    umbra: 0px 1px 5px 0px rgba(0, 0, 0, 0.2),
    penumbra: 0px 2px 2px 0px rgba(0, 0, 0, 0.14),
    ambient: 0px 3px 1px -2px rgba(0, 0, 0, 0.12)
  ),
  level3: (
    umbra: 0px 1px 8px 0px rgba(0, 0, 0, 0.2),
    penumbra: 0px 3px 4px 0px rgba(0, 0, 0, 0.14),
    ambient: 0px 3px 3px -2px rgba(0, 0, 0, 0.12)
  ),
  level4: (
    umbra: 0px 2px 4px -1px rgba(0, 0, 0, 0.2),
    penumbra: 0px 4px 5px 0px rgba(0, 0, 0, 0.14),
    ambient: 0px 1px 10px 0px rgba(0, 0, 0, 0.12)
  ),
  level5: (
    umbra: 0px 3px 5px -1px rgba(0, 0, 0, 0.2),
    penumbra: 0px 6px 10px 0px rgba(0, 0, 0, 0.14),
    ambient: 0px 1px 18px 0px rgba(0, 0, 0, 0.12)
  )
);
```

## 间距系统

### 间距规范
```scss
$spacing-scale: (
  0: 0,
  1: 4px,
  2: 8px,
  3: 12px,
  4: 16px,
  5: 20px,
  6: 24px,
  7: 28px,
  8: 32px,
  9: 36px,
  10: 40px,
  12: 48px,
  14: 56px,
  16: 64px,
  20: 80px,
  24: 96px
);

// 使用示例
.container { padding: spacing(4); }  // 16px
.card { margin-bottom: spacing(3); } // 12px
```

## 栅格系统

### 响应式断点
```scss
$grid-breakpoints: (
  xs: 0,       // < 600px  手机竖屏
  sm: 600px,   // ≥ 600px  手机横屏
  md: 960px,   // ≥ 960px  平板
  lg: 1280px,  // ≥ 1280px 小屏幕桌面
  xl: 1920px   // ≥ 1920px 大屏幕桌面
);

// 栅格配置
$grid-columns: 12;
$grid-gutter-base: 16px;
$grid-margins: (
  xs: 16px,
  sm: 24px,
  md: 24px,
  lg: 24px,
  xl: 24px
);
```

## 组件规范

### 按钮 (Button)
```scss
.md-button {
  // 尺寸
  height: 40px;
  padding: 0 24px;
  
  // 形状
  border-radius: 20px;
  
  // 文字
  font-size: 14px;
  font-weight: 500;
  
  // 状态
  &:hover { /* 悬停效果 */ }
  &:focus { /* 焦点效果 */ }
  &:active { /* 按下效果 */ }
  &:disabled { /* 禁用效果 */ }
}

// 变体
.md-button--elevated { /* 浮起按钮 */ }
.md-button--filled { /* 填充按钮 */ }
.md-button--tonal { /* 色调按钮 */ }
.md-button--outlined { /* 轮廓按钮 */ }
.md-button--text { /* 文字按钮 */ }
```

### 卡片 (Card)
```scss
.md-card {
  // 形状
  border-radius: 12px;
  
  // 表面
  background: var(--md-sys-color-surface);
  
  // 高度
  box-shadow: var(--md-elevation-1);
  
  // 内边距
  padding: 16px;
  
  // 变体
  &--elevated { /* 浮起卡片 */ }
  &--filled { /* 填充卡片 */ }
  &--outlined { /* 轮廓卡片 */ }
}
```

### 输入框 (TextField)
```scss
.md-text-field {
  // 容器
  .md-text-field__container {
    height: 56px;
    border-radius: 4px 4px 0 0;
    background: var(--md-sys-color-surface-variant);
  }
  
  // 标签
  .md-text-field__label {
    font-size: 16px;
    color: var(--md-sys-color-on-surface-variant);
  }
  
  // 输入
  .md-text-field__input {
    font-size: 16px;
    padding: 0 16px;
  }
  
  // 变体
  &--filled { /* 填充样式 */ }
  &--outlined { /* 轮廓样式 */ }
}
```

## 动画规范

### 缓动函数
```scss
$easing: (
  standard: cubic-bezier(0.2, 0.0, 0, 1.0),
  emphasized: cubic-bezier(0.2, 0.0, 0, 1.0),
  emphasized-decelerate: cubic-bezier(0.05, 0.7, 0.1, 1.0),
  emphasized-accelerate: cubic-bezier(0.3, 0.0, 0.8, 0.15),
  standard-decelerate: cubic-bezier(0.0, 0.0, 0, 1.0),
  standard-accelerate: cubic-bezier(0.3, 0.0, 1, 1.0)
);
```

### 持续时间
```scss
$duration: (
  short1: 50ms,
  short2: 100ms,
  short3: 150ms,
  short4: 200ms,
  medium1: 250ms,
  medium2: 300ms,
  medium3: 350ms,
  medium4: 400ms,
  long1: 450ms,
  long2: 500ms,
  long3: 550ms,
  long4: 600ms,
  extra-long1: 700ms,
  extra-long2: 800ms,
  extra-long3: 900ms,
  extra-long4: 1000ms
);
```

## 图标系统

### Material Symbols
```html
<!-- 引入 -->
<link rel="stylesheet" 
      href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined" />

<!-- 使用 -->
<span class="material-symbols-outlined">home</span>

<!-- 配置 -->
<style>
.material-symbols-outlined {
  font-variation-settings:
    'FILL' 0,      /* 填充 0-1 */
    'wght' 400,    /* 粗细 100-700 */
    'GRAD' 0,      /* 渐变 -25-200 */
    'opsz' 24;     /* 尺寸 20-48 */
}
</style>
```

### 图标尺寸
- 小图标：20px
- 默认图标：24px
- 中等图标：40px
- 大图标：48px

## 无障碍设计

### 颜色对比度
- 正常文本：最小 4.5:1
- 大文本：最小 3:1
- 非文本元素：最小 3:1

### 焦点指示器
```scss
.focusable {
  &:focus-visible {
    outline: 2px solid var(--md-sys-color-primary);
    outline-offset: 2px;
  }
}
```

### 触摸目标
- 最小尺寸：48x48px
- 推荐尺寸：56x56px

## 暗黑模式

### 自动切换
```javascript
// 检测系统主题
const prefersDark = window.matchMedia('(prefers-color-scheme: dark)')

// 监听变化
prefersDark.addEventListener('change', (e) => {
  toggleTheme(e.matches ? 'dark' : 'light')
})
```

### 主题切换动画
```scss
// 平滑过渡
* {
  transition: background-color 200ms ease,
              color 200ms ease;
}
```

## 设计交付

### 设计文件规范
- 使用Figma进行设计
- 遵循8px网格系统
- 组件化设计方法
- 标注间距和颜色

### 切图规范
- 2x、3x倍图支持
- WebP格式优先
- SVG矢量图标
- 图片压缩优化

## 设计检查清单

- [ ] 颜色符合Material Design 3规范
- [ ] 字体大小和行高正确
- [ ] 圆角使用统一
- [ ] 间距符合8px网格
- [ ] 投影层级正确
- [ ] 动画流畅自然
- [ ] 响应式适配完整
- [ ] 无障碍支持完善
- [ ] 暗黑模式正常
- [ ] 性能优化到位