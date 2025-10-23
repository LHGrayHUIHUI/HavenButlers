package com.haven.account.dto;

import com.haven.base.model.dto.FamilyDTO;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 账户服务家庭数据传输对象
 * 继承base模块的FamilyDTO功能
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AccFamDTO extends FamilyDTO {

    /**
     * 本地数据库主键ID
     */
    private Long id;

    /**
     * 家庭UUID
     */
    private UUID uuid;

    /**
     * 家庭描述（account特有）
     */
    @Size(max = 500, message = "家庭描述不能超过500个字符")
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 默认构造函数
     */
    public AccFamDTO() {
    }

    /**
     * 构造函数
     */
    public AccFamDTO(String familyName, String ownerId) {
        this.setFamilyName(familyName);
        this.setOwnerId(ownerId);
        this.setStatus("ACTIVE");
    }

    /**
     * 从base FamilyDTO转换
     */
    public static AccFamDTO fromBase(FamilyDTO base) {
        AccFamDTO dto = new AccFamDTO();
        dto.setFamilyId(base.getFamilyId());
        dto.setFamilyName(base.getFamilyName());
        dto.setOwnerId(base.getOwnerId());
        dto.setAddress(base.getAddress());
        dto.setCity(base.getCity());
        dto.setProvince(base.getProvince());
        dto.setCountry(base.getCountry());
        dto.setTimezone(base.getTimezone());
        dto.setMemberCount(base.getMemberCount());
        dto.setDeviceCount(base.getDeviceCount());
        dto.setRoomCount(base.getRoomCount());
        dto.setSceneCount(base.getSceneCount());
        dto.setDeviceQuota(base.getDeviceQuota());
        dto.setMemberQuota(base.getMemberQuota());
        dto.setFamilyType(base.getFamilyType());
        dto.setStatus(base.getStatus());
        dto.setMembers(base.getMembers());
        dto.setRooms(base.getRooms());
        dto.setSettings(base.getSettings());
        dto.setCreateTime(base.getCreateTime());
        dto.setUpdateTime(base.getUpdateTime());
        return dto;
    }

    /**
     * 转换为base FamilyDTO
     */
    public FamilyDTO toBase() {
        FamilyDTO base = new FamilyDTO();
        base.setFamilyId(this.getFamilyId());
        base.setFamilyName(this.getFamilyName());
        base.setOwnerId(this.getOwnerId());
        base.setAddress(this.getAddress());
        base.setCity(this.getCity());
        base.setProvince(this.getProvince());
        base.setCountry(this.getCountry());
        base.setTimezone(this.getTimezone());
        base.setMemberCount(this.getMemberCount());
        base.setDeviceCount(this.getDeviceCount());
        base.setRoomCount(this.getRoomCount());
        base.setSceneCount(this.getSceneCount());
        base.setDeviceQuota(this.getDeviceQuota());
        base.setMemberQuota(this.getMemberQuota());
        base.setFamilyType(this.getFamilyType());
        base.setStatus(this.getStatus());
        base.setMembers(this.getMembers());
        base.setRooms(this.getRooms());
        base.setSettings(this.getSettings());
        base.setCreateTime(this.getCreateTime());
        base.setUpdateTime(this.getUpdateTime());
        return base;
    }

    /**
     * 检查是否为家庭所有者（String版本）
     */
    public boolean isOwner(String userId) {
        return userId != null && userId.equals(getOwnerId());
    }

    /**
     * 检查是否为家庭所有者（Long版本）
     */
    public boolean isOwner(Long userId) {
        return userId != null && userId.toString().equals(getOwnerId());
    }
}