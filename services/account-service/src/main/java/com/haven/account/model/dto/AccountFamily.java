package com.haven.account.model.dto;

import com.haven.base.model.dto.BaseFamilyDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

/**
 * 家庭数据传输对象
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AccountFamily extends BaseFamilyDTO {

    private Long id;
    private UUID uuid;

    @NotBlank(message = "家庭名称不能为空")
    @Size(min = 1, max = 100, message = "家庭名称长度必须在1-100个字符之间")
    private String familyName;

    private String ownerId;

    @Size(max = 500, message = "家庭描述不能超过500个字符")
    private String description;

    private String status;

    private String backgroundImageUrl;

    private List<AccountFamilyMember> members;


    /**
     * 检查是否为家庭所有者
     */
    public boolean isOwner(String userId) {
        return getOwnerId() != null && getOwnerId().equals(userId);
    }

    /**
     * 检查是否为家庭所有者（兼容Long类型）
     */
    public boolean isOwner(Long userId) {
        return userId != null && isOwner(userId.toString());
    }

    /**
     * 获取成员数量
     */
    public int getMemberCount() {
        return members != null ? members.size() : 0;
    }
}