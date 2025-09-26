package com.haven.base.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 分页响应包装器
 * 继承自ResponseWrapper，专门用于分页数据的响应
 *
 * @param <T> 数据类型
 * @author HavenButler
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagedResponseWrapper<T> extends ResponseWrapper<List<T>> {

    /**
     * 分页信息
     */
    private PageInfo pageInfo;

    /**
     * 构造函数
     */
    public PagedResponseWrapper() {
        super();
    }

    /**
     * 构造函数
     */
    @Builder(builderMethodName = "pagedBuilder")
    public PagedResponseWrapper(int code, String message, List<T> data,
                               String traceId, java.time.LocalDateTime timestamp,
                               PageInfo pageInfo) {
        super(code, message, data, traceId, timestamp);
        this.pageInfo = pageInfo;
    }

    /**
     * 成功分页响应
     *
     * @param data 分页数据
     * @param pageInfo 分页信息
     * @return 分页响应
     */
    public static <T> PagedResponseWrapper<T> success(List<T> data, PageInfo pageInfo) {
        return success("操作成功", data, pageInfo);
    }

    /**
     * 成功分页响应（带消息）
     *
     * @param message 响应消息
     * @param data 分页数据
     * @param pageInfo 分页信息
     * @return 分页响应
     */
    public static <T> PagedResponseWrapper<T> success(String message, List<T> data, PageInfo pageInfo) {
        return PagedResponseWrapper.<T>pagedBuilder()
                .code(0)
                .message(message)
                .data(data)
                .pageInfo(pageInfo)
                .traceId(org.slf4j.MDC.get("traceId"))
                .timestamp(java.time.LocalDateTime.now())
                .build();
    }

    /**
     * 从PageResponse创建分页响应
     *
     * @param pageResponse 分页响应对象
     * @return 分页响应包装器
     */
    public static <T> PagedResponseWrapper<T> fromPageResponse(
            com.haven.base.model.dto.PageResponse<T> pageResponse) {
        PageInfo pageInfo = PageInfo.builder()
                .page(pageResponse.getPage())
                .size(pageResponse.getSize())
                .total(pageResponse.getTotal())
                .totalPages(pageResponse.getTotalPage())
                .hasNext(pageResponse.getHasNext())
                .hasPrevious(pageResponse.getHasPrevious())
                .build();

        return success(pageResponse.getList(), pageInfo);
    }

    /**
     * 空分页响应
     *
     * @param page 页码
     * @param size 页大小
     * @return 空分页响应
     */
    public static <T> PagedResponseWrapper<T> empty(int page, int size) {
        PageInfo pageInfo = PageInfo.builder()
                .page(page)
                .size(size)
                .total(0L)
                .totalPages(0)
                .hasNext(false)
                .hasPrevious(page > 1)
                .build();

        return success(java.util.Collections.emptyList(), pageInfo);
    }

    /**
     * 分页信息
     */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PageInfo {
        /**
         * 当前页码（从1开始）
         */
        private int page;

        /**
         * 每页大小
         */
        private int size;

        /**
         * 总记录数
         */
        private long total;

        /**
         * 总页数
         */
        private int totalPages;

        /**
         * 是否有下一页
         */
        private boolean hasNext;

        /**
         * 是否有上一页
         */
        private boolean hasPrevious;

        /**
         * 是否为第一页
         */
        public boolean isFirst() {
            return page == 1;
        }

        /**
         * 是否为最后一页
         */
        public boolean isLast() {
            return page == totalPages;
        }

        /**
         * 是否为空页（无数据）
         */
        public boolean isEmpty() {
            return total == 0;
        }

        /**
         * 获取数据起始位置（从0开始）
         */
        public long getOffset() {
            return (long) (page - 1) * size;
        }

        /**
         * 获取数据结束位置（从0开始）
         */
        public long getEndOffset() {
            return Math.min(getOffset() + size, total) - 1;
        }
    }
}