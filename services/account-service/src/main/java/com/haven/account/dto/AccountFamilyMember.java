package com.haven.account.dto;

import com.haven.base.model.dto.BaseFamilyMember;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 家庭成员数据传输对象
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AccountFamilyMember extends BaseFamilyMember  {

    private Long id;

    private Long familyId;

    private String username;

    private String email;

    private LocalDateTime joinedAt;

}