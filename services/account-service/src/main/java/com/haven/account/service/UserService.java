package com.haven.account.service;

import com.haven.account.dto.*;
import com.haven.account.entity.User;
import com.haven.account.repository.UserRepository;
import com.haven.account.security.JwtTokenService;
import com.haven.base.common.exception.BusinessException;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.utils.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 用户服务类
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    /**
     * 用户注册
     */
    @Transactional
    public UserInfoDTO register(RegisterRequest request) {
        log.info("开始用户注册: {}", request.getUsername());

        // 1. 验证注册请求
        validateRegisterRequest(request);

        // 2. 检查用户名和邮箱是否已存在
        checkUserExists(request.getUsername(), request.getEmail(), request.getPhone());

        // 3. 创建用户实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setUserStatus(User.Status.ACTIVE);
        user.setRoles("USER");

        // 4. 保存用户
        User savedUser = userRepository.save(user);

        log.info("用户注册成功: {}, ID: {}", savedUser.getUsername(), savedUser.getId());

        // 5. 转换为DTO返回
        return convertToDTO(savedUser);
    }

    /**
     * 用户登录 - 支持手机号优先登录
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String loginIdentifier = request.getPhone() != null && !request.getPhone().isEmpty()
                ? request.getPhone()
                : request.getUsername();

        log.info("开始用户登录: {}", loginIdentifier);

        // 1. 验证登录请求
        validateLoginRequest(request);

        // 2. 查找用户 - 优先手机号，再用户名，最后邮箱
        User user = findUserByPhoneOrUsernameOrEmail(loginIdentifier)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "用户不存在"));

        // 3. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("用户登录密码错误: {}", loginIdentifier);
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "手机号或密码错误");
        }

        // 4. 检查用户状态
        if (!User.Status.ACTIVE.equals(user.getUserStatus())) {
            log.warn("用户状态异常: {}, 状态: {}", loginIdentifier, user.getStatus());
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户账户已被禁用");
        }

        // 5. 生成JWT Token
        JwtTokenService.TokenPair tokenPair = jwtTokenService.generateTokenPair(user);

        // 6. 更新最后登录时间
        user.setUpdateTime(LocalDateTime.now());
        userRepository.save(user);

        log.info("用户登录成功: {}", loginIdentifier);

        // 7. 构建响应
        UserInfoDTO userDTO = convertToDTO(user);
        LoginResponse response = new LoginResponse(tokenPair.getAccessToken(), tokenPair.getRefreshToken(), userDTO);
        response.setExpiresInHours(8); // 2小时过期

        return response;
    }

    /**
     * 根据ID获取用户信息
     */
    public UserInfoDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "用户不存在"));
        return convertToDTO(user);
    }

    /**
     * 根据手机号或用户名或邮箱获取用户 - 手机号优先
     */
    public Optional<User> findUserByPhoneOrUsernameOrEmail(String identifier) {
        // 1. 先尝试按手机号查找
        if (ValidationUtil.isValidPhone(identifier)) {
            Optional<User> user = userRepository.findByPhone(identifier);
            if (user.isPresent()) {
                return user;
            }
        }

        // 2. 再按用户名查找
        Optional<User> user = userRepository.findByUsername(identifier);
        if (user.isPresent()) {
            return user;
        }

        // 3. 最后按邮箱查找
        return userRepository.findByEmail(identifier);
    }

    /**
     * 根据用户名或邮箱获取用户（兼容旧版本）
     */
    public Optional<User> findUserByUsernameOrEmail(String usernameOrEmail) {
        // 先按用户名查找
        Optional<User> user = userRepository.findByUsername(usernameOrEmail);
        if (user.isPresent()) {
            return user;
        }

        // 再按邮箱查找
        return userRepository.findByEmail(usernameOrEmail);
    }

    /**
     * 验证注册请求
     */
    private void validateRegisterRequest(RegisterRequest request) {
        // 验证密码一致性
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的密码不一致");
        }

        // 验证手机号格式（必须提供）
        if (request.getPhone() == null || request.getPhone().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "手机号不能为空");
        }

        if (!ValidationUtil.isValidPhone(request.getPhone())) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT_ERROR, "手机号格式不正确");
        }

        // 验证邮箱格式（如果提供）
        if (request.getEmail() != null && !request.getEmail().isEmpty()
                && !ValidationUtil.isValidEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT_ERROR, "邮箱格式不正确");
        }

        // 验证用户名格式（如果提供）
        if (request.getUsername() != null && !request.getUsername().isEmpty()
                && !ValidationUtil.isValidUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT_ERROR, "用户名格式不正确");
        }
    }

    /**
     * 验证登录请求
     */
    private void validateLoginRequest(LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "用户名不能为空");
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "密码不能为空");
        }
    }

    /**
     * 检查用户是否已存在
     */
    private void checkUserExists(String username, String email, String phone) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户名已存在");
        }

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "邮箱已存在");
        }

        if (phone != null && !phone.isEmpty() && userRepository.existsByPhone(phone)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "手机号已存在");
        }
    }

    /**
     * 转换为DTO
     */
    private UserInfoDTO convertToDTO(User user) {
        UserInfoDTO dto = new UserInfoDTO();
        dto.setId(user.getId());
        dto.setUuid(user.getUuid());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setStatus(user.getStatus());
        dto.setCurrentFamilyId(user.getCurrentFamilyId());
        dto.setRoles(user.getRoles());
        dto.setCreatedAt(user.getCreateTime());  // 使用 BaseEntity 的时间字段
        dto.setUpdatedAt(user.getUpdateTime());  // 使用 BaseEntity 的时间字段
        return dto;
    }

  }