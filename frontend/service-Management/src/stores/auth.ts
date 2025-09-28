import { defineStore } from 'pinia';

// 认证状态存储（Basic 串存于 sessionStorage，关闭浏览器失效）
export const useAuthStore = defineStore('auth', {
  state: () => ({
    /** Basic 凭据，如：`Basic base64(user:pass)` */
    basicToken: (sessionStorage.getItem('basicToken') || '') as string,
    /** 登录用户名（用于展示，可选） */
    username: '' as string
  }),
  actions: {
    /** 设置凭据并持久化到 sessionStorage */
    setToken(token: string, username?: string) {
      this.basicToken = token;
      if (username) this.username = username;
      sessionStorage.setItem('basicToken', token);
    },
    /** 清除凭据并退出 */
    clear() {
      this.basicToken = '';
      this.username = '';
      sessionStorage.removeItem('basicToken');
    }
  }
});

