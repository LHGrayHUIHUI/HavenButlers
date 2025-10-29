package com.haven.base.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 家庭数据传输对象
 * 在服务间传递家庭信息的通用模型
 *
 * @author HavenButler
 */
@Data
@MappedSuperclass
public abstract class BaseFamilyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 家庭ID
     */
    private String familyId;

    /**
     * 家庭名称
     */
    private String familyName;

    /**
     * 家庭创建者/管理员ID
     */
    private String ownerId;

    /**
     * 家庭地址
     */
    private String address;

    /**
     * 城市
     */
    private String city;

    /**
     * 省份
     */
    private String province;

    /**
     * 国家
     */
    private String country;

    /**
     * 时区
     */
    private String timezone;

    /**
     * 家庭成员数量
     */
    private Integer memberCount;

    /**
     * 设备数量
     */
    private Integer deviceCount;

    /**
     * 房间数量
     */
    private Integer roomCount;

    /**
     * 场景数量
     */
    private Integer sceneCount;

    /**
     * 设备配额
     */
    private Integer deviceQuota;

    /**
     * 成员配额
     */
    private Integer memberQuota;

    /**
     * 家庭类型（NORMAL/PREMIUM）
     */
    private String familyType;

    /**
     * 家庭状态（ACTIVE/INACTIVE/SUSPENDED）
     */
    private String status;

    /**
     * 家庭成员列表
     */
    private List<BaseFamilyMember> members;

    /**
     * 房间列表
     */
    private List<BaseRoomDTO> rooms;

    /**
     * 家庭设置（JSON）
     */
    private Map<String, Object> settings;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 是否激活
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    /**
     * 是否为高级家庭
     */
    public boolean isPremium() {
        return "PREMIUM".equals(familyType);
    }

    /**
     * 是否达到设备上限
     */
    public boolean isDeviceQuotaReached() {
        return deviceCount != null && deviceQuota != null &&
               deviceCount >= deviceQuota;
    }

    /**
     * 是否达到成员上限
     */
    public boolean isMemberQuotaReached() {
        return memberCount != null && memberQuota != null &&
               memberCount >= memberQuota;
    }

    /**
     * 获取某个成员
     */
    public BaseFamilyMember getMember(String userId) {
        if (members == null) {
            return null;
        }
        return members.stream()
                .filter(m -> userId.equals(m.getUserId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 检查用户是否为成员
     */
    public boolean hasMember(String userId) {
        return getMember(userId) != null;
    }

    /**
     * 检查用户是否为管理员
     */
    public boolean isAdmin(String userId) {
        BaseFamilyMember member = getMember(userId);
        return member != null && member.isAdmin();
    }
}