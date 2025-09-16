package com.haven.base.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应结果
 *
 * @param <T> 数据类型
 * @author HavenButler
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    private List<T> list;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Integer totalPage;

    /**
     * 当前页码
     */
    private Integer page;

    /**
     * 每页大小
     */
    private Integer size;

    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;

    /**
     * 是否有下一页
     */
    private Boolean hasNext;

    /**
     * 创建分页响应
     */
    public static <T> PageResponse<T> of(List<T> list, Long total, PageRequest pageRequest) {
        return of(list, total, pageRequest.getPage(), pageRequest.getSize());
    }

    /**
     * 创建分页响应
     */
    public static <T> PageResponse<T> of(List<T> list, Long total, Integer page, Integer size) {
        int totalPage = (int) Math.ceil((double) total / size);

        return PageResponse.<T>builder()
                .list(list)
                .total(total)
                .totalPage(totalPage)
                .page(page)
                .size(size)
                .hasPrevious(page > 1)
                .hasNext(page < totalPage)
                .build();
    }

    /**
     * 创建空的分页响应
     */
    public static <T> PageResponse<T> empty() {
        return PageResponse.<T>builder()
                .list(List.of())
                .total(0L)
                .totalPage(0)
                .page(1)
                .size(20)
                .hasPrevious(false)
                .hasNext(false)
                .build();
    }

    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return list == null || list.isEmpty();
    }

    /**
     * 是否不为空
     */
    public boolean isNotEmpty() {
        return !isEmpty();
    }

    /**
     * 获取数据条数
     */
    public int getDataSize() {
        return list == null ? 0 : list.size();
    }

    /**
     * 获取开始索引
     */
    public int getStartIndex() {
        return (page - 1) * size + 1;
    }

    /**
     * 获取结束索引
     */
    public int getEndIndex() {
        return Math.min(page * size, total.intValue());
    }
}