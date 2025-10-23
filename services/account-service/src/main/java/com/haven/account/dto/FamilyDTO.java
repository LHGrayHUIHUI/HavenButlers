package com.haven.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 家庭数据传输对象
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Data
public class FamilyDTO {

    private Long id;
    private UUID uuid;

    @NotBlank(message = "家庭名称不能为空")
    @Size(min = 1, max = 100, message = "家庭名称长度必须在1-100个字符之间")
    private String name;

    private Long ownerId;

    @Size(max = 500, message = "家庭描述不能超过500个字符")
    private String description;

    private String status;

    private List<FamilyMemberDTO> members;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 默认构造函数
     */
    public FamilyDTO() {
    }

    /**
     * 构造函数
     */
    public FamilyDTO(String name, Long ownerId) {
        this.name = name;
        this.ownerId = ownerId;
        this.status = "ACTIVE";
    }

    /**
     * 检查是否为家庭所有者
     */
    public boolean isOwner(Long userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    /**
     * 获取成员数量
     */
    public int getMemberCount() {
        return members != null ? members.size() : 0;
    }
}