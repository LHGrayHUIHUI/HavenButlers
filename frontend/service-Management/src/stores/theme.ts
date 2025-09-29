import { ref, watch } from 'vue';
import { defineStore } from 'pinia';

export const useThemeStore = defineStore('theme', () => {
  // 主题状态
  const theme = ref<'light' | 'dark' | 'auto'>('light');

  // 应用主题到document
  const applyTheme = (themeValue: string) => {
    const root = document.documentElement;
    if (themeValue === 'dark') {
      root.setAttribute('data-theme', 'dark');
    } else if (themeValue === 'light') {
      root.setAttribute('data-theme', 'light');
    } else {
      // auto mode - check system preference
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      root.setAttribute('data-theme', prefersDark ? 'dark' : 'light');
    }
  };

  // 设置主题
  const setTheme = (newTheme: 'light' | 'dark' | 'auto') => {
    theme.value = newTheme;
    applyTheme(newTheme);

    // 更新localStorage中的settings
    const settings = localStorage.getItem('settings');
    if (settings) {
      try {
        const settingsObj = JSON.parse(settings);
        settingsObj.theme = newTheme;
        localStorage.setItem('settings', JSON.stringify(settingsObj));
      } catch (e) {
        localStorage.setItem('settings', JSON.stringify({ theme: newTheme }));
      }
    } else {
      localStorage.setItem('settings', JSON.stringify({ theme: newTheme }));
    }
  };

  // 切换主题（用于header的快速切换）
  const toggleTheme = () => {
    const newTheme = theme.value === 'light' ? 'dark' : 'light';
    setTheme(newTheme);
  };

  // 初始化主题
  const initTheme = () => {
    const saved = localStorage.getItem('settings');
    let savedTheme: 'light' | 'dark' | 'auto' = 'light';

    if (saved) {
      try {
        const settings = JSON.parse(saved);
        savedTheme = settings.theme || 'light';
      } catch (e) {
        console.error('Failed to parse settings:', e);
      }
    }

    theme.value = savedTheme;
    applyTheme(savedTheme);
  };

  // 监听系统主题变化（用于auto模式）
  if (window.matchMedia) {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    mediaQuery.addEventListener('change', () => {
      if (theme.value === 'auto') {
        applyTheme('auto');
      }
    });
  }

  return {
    theme,
    setTheme,
    toggleTheme,
    initTheme,
    applyTheme
  };
});