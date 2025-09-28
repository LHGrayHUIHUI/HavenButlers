/**
 * SSE 连接与降级轮询工具
 * - 优先使用 EventSource 连接服务端事件流
 * - 失败或 401 时自动关闭并回退为固定间隔轮询
 */
export interface SSEOptions<T> {
  onMessage: (data: T) => void;
  onError?: (err: any) => void;
  poll?: () => Promise<T>;
  intervalMs?: number; // 轮询间隔，默认 5000ms
}

export function connectSSE<T = any>(url: string, options: SSEOptions<T>) {
  const { onMessage, onError, poll, intervalMs = Number(import.meta.env.VITE_REFRESH_INTERVAL || 5000) } = options;

  let es: EventSource | null = null;
  let timer: number | null = null;

  // 启动轮询函数（降级路径）
  const startPolling = () => {
    if (!poll) return;
    stop();
    timer = window.setInterval(async () => {
      try {
        const data = await poll();
        onMessage(data);
      } catch (e) {
        onError?.(e);
      }
    }, intervalMs);
  };

  // 启动 SSE 连接
  try {
    es = new EventSource(url);
    es.onmessage = (e) => {
      try {
        const data = JSON.parse(e.data);
        onMessage(data);
      } catch (err) {
        onError?.(err);
      }
    };
    es.onerror = (err) => {
      // 发生错误时，关闭 SSE 并回退为轮询
      es?.close();
      es = null;
      onError?.(err);
      startPolling();
    };
  } catch (err) {
    // 构造失败则直接轮询
    onError?.(err);
    startPolling();
  }

  const stop = () => {
    if (es) {
      es.close();
      es = null;
    }
    if (timer) {
      clearInterval(timer);
      timer = null;
    }
  };

  return { stop };
}

