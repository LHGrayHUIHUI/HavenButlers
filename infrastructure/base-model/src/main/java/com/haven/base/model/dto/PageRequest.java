package com.haven.base.model.dto;

import com.haven.base.common.constants.SystemConstants;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;

/**
 * 分页请求参数
 *
 * @author HavenButler
 */
@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码（从1开始）
     */
    @Min(value = 1, message = "页码最小值为1")
    private Integer page = SystemConstants.Page.DEFAULT_PAGE;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小最小值为1")
    @Max(value = SystemConstants.Page.MAX_SIZE, message = "每页大小最大值为" + SystemConstants.Page.MAX_SIZE)
    private Integer size = SystemConstants.Page.DEFAULT_SIZE;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方式（ASC/DESC）
     */
    private String sortOrder = "DESC";

    /**
     * 搜索关键字
     */
    private String keyword;

    /**
     * 获取偏移量
     */
    public int getOffset() {
        return (page - 1) * size;
    }

    /**
     * 构造方法
     */
    public PageRequest() {
    }

    public PageRequest(Integer page, Integer size) {
        this.page = page != null ? page : SystemConstants.Page.DEFAULT_PAGE;
        this.size = size != null ? size : SystemConstants.Page.DEFAULT_SIZE;
    }

    /**
     * 创建默认分页请求
     */
    public static PageRequest of(Integer page, Integer size) {
        return new PageRequest(page, size);
    }

    /**
     * 创建默认分页请求
     */
    public static PageRequest defaultPage() {
        return new PageRequest();
    }

    /**
     * 是否需要排序
     */
    public boolean needSort() {
        return sortField != null && !sortField.trim().isEmpty();
    }

    /**
     * 是否为升序
     */
    public boolean isAsc() {
        return "ASC".equalsIgnoreCase(sortOrder);
    }

    /**
     * 获取MySQL LIMIT语句
     */
    public String getLimitSql() {
        return String.format(" LIMIT %d, %d", getOffset(), size);
    }

    /**
     * 获取排序SQL
     */
    public String getOrderBySql() {
        if (!needSort()) {
            return "";
        }
        return String.format(" ORDER BY %s %s", sortField, sortOrder.toUpperCase());
    }
}