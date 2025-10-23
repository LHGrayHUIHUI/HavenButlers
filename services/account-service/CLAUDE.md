# account-service CLAUDE.md

## 模块概述
account-service是HavenButler平台的用户账户核心服务，负责用户注册登录、家庭权限管理、设备访问控制等核心认证授权功能。作为安全的第一道防线，必须保证数据安全和访问控制的严密性。

## 开发指导原则

### 1. 核心设计原则
- **安全优先**：所有用户数据必须加密存储，权限检查严密
- **无状态设计**：使用JWT Token，支持水平扩展
- **多租户隔离**：基于家庭ID进行数据隔离
- **细粒度权限**：支持家庭、房间、设备三级权限控制

### 2. 数据存储架构

#### 2.1 数据库直连访问
```java
/**
 * 用户数据直接访问MySQL数据库
 * storage-service仅用于文件存储操作
 */
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileServiceClient fileServiceClient; // 仅用于文件操作

    public User createUser(UserDTO userDTO) {
        // 1. 密码加密
        userDTO.setPassword(BCrypt.hashpw(userDTO.getPassword()));

        // 2. 用户数据直接保存到数据库
        User user = userRepository.save(userDTO.toEntity());

        // 3. 如果涉及文件操作，调用storage-service
        if (userDTO.getAvatarFile() != null) {
            String avatarUrl = fileServiceClient.uploadFile(userDTO.getAvatarFile());
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);
        }

        return user;
    }
}
```

#### 2.2 storage-service集成范围
```java
/**
 * storage-service仅处理文件相关操作
 */
@Component
public class FileServiceClient {

    /**
     * 上传用户头像
     */
    public String uploadUserAvatar(MultipartFile file, String userId) {
        // 调用storage-service上传文件
        FileUploadRequest request = FileUploadRequest.builder()
            .bucket("user-avatars")
            .objectName(userId + "/avatar.jpg")
            .file(file)
            .build();

        return storageClient.uploadFile(request).getFileUrl();
    }

    /**
     * 上传家庭图片
     */
    public String uploadFamilyImage(MultipartFile file, String familyId) {
        FileUploadRequest request = FileUploadRequest.builder()
            .bucket("family-images")
            .objectName(familyId + "/" + UUID.randomUUID() + ".jpg")
            .file(file)
            .build();

        return storageClient.uploadFile(request).getFileUrl();
    }
}
```

### 3. 权限模型实现

#### 3.1 权限层级定义
```java
/**
 * 三级权限模型
 * 家庭 -> 房间 -> 设备
 */
public enum FamilyRole {
    ADMIN("family_admin", "家庭管理员", 100),
    MEMBER("family_member", "家庭成员", 50),
    GUEST("family_guest", "访客", 10);
}

public enum RoomPermission {
    OWNER("room_owner", "房间所有者"),
    USER("room_user", "使用者"),
    VIEWER("room_viewer", "查看者");
}

public enum DevicePermission {
    CONTROL("device_control", "完全控制"),
    USE("device_use", "使用权限"),
    READ("device_read", "只读权限");
}
```

#### 3.2 权限检查逻辑
```java
@Component
public class PermissionChecker {

    /**
     * 检查用户对设备的权限
     */
    public boolean checkDevicePermission(String userId, String deviceId, String operation) {
        // 1. 检查用户家庭角色
        FamilyRole familyRole = getUserFamilyRole(userId);
        if (familyRole == FamilyRole.ADMIN) {
            return true; // 管理员拥有所有权限
        }

        // 2. 获取设备所在房间
        String roomId = getDeviceRoom(deviceId);

        // 3. 检查房间权限
        RoomPermission roomPerm = getUserRoomPermission(userId, roomId);
        if (roomPerm == null) {
            return false; // 无房间权限
        }

        // 4. 检查设备权限
        DevicePermission devicePerm = getUserDevicePermission(userId, deviceId);

        // 5. 综合判断操作权限
        return hasPermissionForOperation(devicePerm, operation);
    }
}
```

### 4. JWT Token管理

#### 4.1 Token生成
```java
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final long ACCESS_TOKEN_VALIDITY = 2 * 60 * 60 * 1000; // 2小时
    private static final long REFRESH_TOKEN_VALIDITY = 7 * 24 * 60 * 60 * 1000; // 7天

    public TokenPair generateTokenPair(User user) {
        Date now = new Date();

        // Access Token
        String accessToken = Jwts.builder()
            .setSubject(user.getId())
            .claim("name", user.getName())
            .claim("familyId", user.getCurrentFamilyId())
            .claim("role", user.getFamilyRole())
            .claim("permissions", user.getPermissions())
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + ACCESS_TOKEN_VALIDITY))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();

        // Refresh Token
        String refreshToken = Jwts.builder()
            .setSubject(user.getId())
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + REFRESH_TOKEN_VALIDITY))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();

        // 保存到Redis（直接访问Redis）
        saveTokenToRedis(user.getId(), accessToken, refreshToken);

        return new TokenPair(accessToken, refreshToken);
    }
}
```

#### 4.2 Token黑名单机制
```java
@Component
public class TokenBlacklistService {

    /**
     * 将用户Token加入黑名单
     */
    public void addToBlacklist(String token) {
        // 获取Token过期时间
        Date expiration = getExpirationFromToken(token);
        long ttl = expiration.getTime() - System.currentTimeMillis();

        if (ttl > 0) {
            // 直接保存到Redis
            redisTemplate.opsForValue().set("blacklist:" + token, "1", ttl, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 检查Token是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        String value = redisTemplate.opsForValue().get("blacklist:" + token);
        return value != null;
    }
}
```

### 5. 家庭管理功能

#### 5.1 家庭创建流程
```java
@Service
public class FamilyService {

    /**
     * 创建新家庭
     */
    @Transactional
    public Family createFamily(String userId, FamilyDTO familyDTO) {
        // 1. 创建家庭
        Family family = new Family();
        family.setName(familyDTO.getName());
        family.setOwnerId(userId);
        family.setCreatedAt(new Date());

        // 2. 保存家庭信息
        family = saveFamilyToStorage(family);

        // 3. 添加用户为管理员
        addFamilyMember(family.getId(), userId, FamilyRole.ADMIN);

        // 4. 创建默认房间
        createDefaultRooms(family.getId());

        // 5. 初始化权限
        initializePermissionTree(family.getId());

        return family;
    }

    /**
     * 邀请家庭成员
     */
    public String inviteMember(String familyId, String inviterId, String email) {
        // 1. 检查邀请权限
        if (!hasInvitePermission(inviterId, familyId)) {
            throw new UnauthorizedException("无邀请权限");
        }

        // 2. 生成邀请码
        String inviteCode = generateInviteCode();

        // 3. 保存邀请信息
        saveInvitation(familyId, email, inviteCode);

        // 4. 发送邀请邮件（调用message-service）
        sendInvitationEmail(email, inviteCode);

        return inviteCode;
    }
}
```

### 6. 安全防护措施

#### 6.1 登录安全
```java
@Component
public class LoginSecurityService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    /**
     * 登录失败处理
     */
    public void handleLoginFailure(String username) {
        String key = "login_attempts:" + username;

        // 递增失败次数
        Integer attempts = incrementAttempts(key);

        if (attempts >= MAX_ATTEMPTS) {
            // 锁定账户
            lockAccount(username, LOCK_DURATION_MINUTES);

            // 发送警报
            sendSecurityAlert(username, "账户被锁定，登录失败次数过多");
        }
    }

    /**
     * 异常登录检测
     */
    public void detectAnomalousLogin(String userId, LoginInfo loginInfo) {
        // 1. 检查登录地点
        if (isUnusualLocation(userId, loginInfo.getIp())) {
            sendSecurityAlert(userId, "异常地点登录");
        }

        // 2. 检查登录时间
        if (isUnusualTime(userId, loginInfo.getTime())) {
            sendSecurityAlert(userId, "异常时间登录");
        }

        // 3. 新设备检测
        if (isNewDevice(userId, loginInfo.getDeviceId())) {
            // 要求二次验证
            requireTwoFactorAuth(userId);
        }
    }
}
```

#### 6.2 敏感操作保护
```java
@Component
public class SensitiveOperationGuard {

    /**
     * 敏感操作需要二次验证
     */
    @SecondFactorAuth
    public void performSensitiveOperation(String userId, String operation) {
        // 敏感操作包括但不限于：
        // - 删除家庭
        // - 移除成员
        // - 修改管理员
        // - 重置权限
        // - 删除设备
    }
}
```

### 7. 性能优化

#### 7.1 缓存策略
```java
@Component
public class UserCacheService {

    /**
     * 多级缓存：本地缓存 + Redis
     */
    @Cacheable(value = "users", key = "#userId")
    public User getUser(String userId) {
        // 1. 本地缓存（Caffeine）
        // 2. Redis缓存
        // 3. 数据库查询
        return loadUserFromStorage(userId);
    }

    /**
     * 权限信息缓存
     */
    @Cacheable(value = "permissions", key = "#userId + ':' + #resourceId")
    public Permission getUserPermission(String userId, String resourceId) {
        return loadPermissionFromStorage(userId, resourceId);
    }
}
```

### 8. 监控指标

#### 8.1 关键指标
```java
@Component
public class AccountMetrics {

    @Autowired
    private MeterRegistry registry;

    // 登录指标
    public void recordLoginAttempt(boolean success) {
        registry.counter("account.login",
            "status", success ? "success" : "failure").increment();
    }

    // Token刷新指标
    public void recordTokenRefresh() {
        registry.counter("account.token.refresh").increment();
    }

    // 权限检查耗时
    @Timed("account.permission.check")
    public boolean checkPermission(String userId, String resource) {
        // 权限检查逻辑
    }
}
```

### 9. 开发注意事项

#### 必须做的事
- 密码必须使用BCrypt加密
- 所有敏感操作需要记录日志
- Token必须设置合理过期时间
- 实现登录失败锁定机制
- 权限检查必须严格执行

#### 不能做的事
- 不能明文存储密码
- 不能跳过权限检查
- 不能在日志中记录Token
- 不能忽略异常登录
- 不能将权限信息写入日志

### 10. 测试要求
- 权限测试覆盖率≥95%
- Token相关测试≥90%
- 家庭功能测试≥85%
- 安全功能集成测试
- 权限边界测试