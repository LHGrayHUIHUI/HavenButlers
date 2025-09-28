import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import zhCn from 'element-plus/dist/locale/zh-cn.mjs';
import App from './App.vue';
import router from './router';

// Import Element Plus CSS first
import 'element-plus/dist/index.css';

// Then import our custom styles to override
import './styles/variables.css';
import './styles/layout.css';
import './styles/utilities.css';

// 创建应用并挂载
const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);
app.use(ElementPlus, { locale: zhCn });

// 初始化主题（在pinia创建后）
import { useThemeStore } from './stores/theme';
const themeStore = useThemeStore(pinia);
themeStore.initTheme();

app.mount('#app');
