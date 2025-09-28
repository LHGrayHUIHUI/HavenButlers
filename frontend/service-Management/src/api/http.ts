import axios from 'axios';
import { useAuthStore } from '@/stores/auth';

// 统一 HTTP 客户端配置与拦截器
// - Base URL：默认同源 `/api`，可用 VITE_API_BASE_URL 覆盖
// - 认证：自动注入 Authorization: Basic ...
// - 错误：统一将后端 AdminResponse 错误抛出

export interface AdminResponse<T> {
  success: boolean
  code: number
  message: string
  data: T
  traceId?: string
  timestamp?: number
}

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';

export const http = axios.create({ baseURL, timeout: 15000 });

http.interceptors.request.use((config) => {
  // 在请求发送前注入 Basic 凭据
  const auth = useAuthStore();
  if (auth.basicToken) {
    config.headers = config.headers || {};
    config.headers['Authorization'] = auth.basicToken;
  }
  return config;
});

http.interceptors.response.use(
  (resp) => {
    // 处理统一包装结构（AdminResponse）
    const data = resp.data as AdminResponse<unknown> | unknown;
    if (data && typeof data === 'object' && 'success' in (data as any)) {
      const w = data as AdminResponse<any>;
      if (!w.success) {
        // 非成功状态直接抛错，便于调用侧统一提示
        return Promise.reject(new Error(w.message || '请求失败'));
      }
      // 返回 data 字段给调用方
      return { ...resp, data: (w.data as any) };
    }
    return resp;
  },
  (error) => {
    // 401 处理：清除凭据并重定向登录
    if (error?.response?.status === 401) {
      try {
        const auth = useAuthStore();
        auth.clear();
        // 仅在浏览器环境下进行跳转
        if (typeof window !== 'undefined') window.location.hash = '#/login';
      } catch (_) {
        // 忽略 store 初始化异常
      }
    }
    return Promise.reject(error);
  }
);
