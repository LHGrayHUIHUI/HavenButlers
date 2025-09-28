/**
 * 分页结构适配器：兼容旧契约 { list, total, totalPage, page, size, hasPrevious, hasNext }
 * 以及新契约 { content, totalElements, totalPages, number, size }
 * 注意：UI 层统一依赖 list/total/totalPage/page/size/hasPrevious/hasNext
 */
export function adaptPage<T = any>(p: any) {
  const page = p?.page ?? p?.number ?? 1;
  const totalPage = p?.totalPage ?? p?.totalPages ?? 0;
  const size = p?.size ?? 0;
  const list: T[] = p?.list ?? p?.content ?? [];
  const total = p?.total ?? p?.totalElements ?? list.length ?? 0;
  const hasPrevious = p?.hasPrevious ?? page > 1;
  const hasNext =
    p?.hasNext ?? (typeof totalPage === 'number' && totalPage > 0 ? page < totalPage : false);

  return { list, total, totalPage, page, size, hasPrevious, hasNext } as {
    list: T[];
    total: number;
    totalPage: number;
    page: number;
    size: number;
    hasPrevious: boolean;
    hasNext: boolean;
  };
}

