package com.haven.admin.model;

/**
 * 分页请求DTO
 * 替代base-model的PageRequest
 */
public class PageRequest {
    private int page = 0;
    private int size = 20;
    private String sort;

    public PageRequest() {}

    public PageRequest(int page, int size) {
        this.page = page;
        this.size = size;
    }

    // Getters and Setters
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
}