package com.haven.admin.model;

import java.util.List;

/**
 * 分页响应DTO
 * 替代base-model的PageResponse
 */
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long total;
    private int totalPages;

    public PageResponse() {}

    public PageResponse(List<T> content, int page, int size, long total) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.total = total;
        this.totalPages = (int) Math.ceil((double) total / size);
    }

    // 静态工厂方法
    public static <T> PageResponse<T> of(List<T> content, long total, PageRequest pageRequest) {
        return new PageResponse<>(content, pageRequest.getPage(), pageRequest.getSize(), total);
    }

    // Getters and Setters
    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}