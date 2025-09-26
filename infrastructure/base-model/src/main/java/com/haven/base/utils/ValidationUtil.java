package com.haven.base.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * 参数校验工具类
 * 提供常用的参数校验方法
 *
 * @author HavenButler
 */
public final class ValidationUtil {

    // 正则表达式模式
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$");
    private static final Pattern IP_V4_PATTERN = Pattern.compile("^(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");
    private static final Pattern MAC_ADDRESS_PATTERN = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?|ftp)://[\\w-]+(\\.[\\w-]+)+([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{3,19}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@#$%^&*()_+]{8,32}$");
    private static final Pattern CHINESE_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5]+$");
    private static final Pattern CHINESE_NAME_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5]{2,10}$");

    private ValidationUtil() {
        throw new AssertionError("不允许实例化");
    }

    /**
     * 验证手机号
     *
     * @param phone 手机号
     * @return 是否有效
     */
    public static boolean isValidPhone(String phone) {
        if (StringUtils.isBlank(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * 验证邮箱
     *
     * @param email 邮箱
     * @return 是否有效
     */
    public static boolean isValidEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.toLowerCase()).matches();
    }

    /**
     * 验证身份证号（18位）
     *
     * @param idCard 身份证号
     * @return 是否有效
     */
    public static boolean isValidIdCard(String idCard) {
        if (StringUtils.isBlank(idCard)) {
            return false;
        }
        if (!ID_CARD_PATTERN.matcher(idCard).matches()) {
            return false;
        }
        // 校验码验证
        return validateIdCardChecksum(idCard);
    }

    /**
     * 验证身份证校验码
     */
    private static boolean validateIdCardChecksum(String idCard) {
        char[] chars = idCard.toCharArray();
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] checksumChars = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (chars[i] - '0') * weights[i];
        }

        char expectedChecksum = checksumChars[sum % 11];
        char actualChecksum = Character.toUpperCase(chars[17]);

        return expectedChecksum == actualChecksum;
    }

    /**
     * 验证IPv4地址
     *
     * @param ip IP地址
     * @return 是否有效
     */
    public static boolean isValidIpV4(String ip) {
        if (StringUtils.isBlank(ip)) {
            return false;
        }
        return IP_V4_PATTERN.matcher(ip).matches();
    }

    /**
     * 验证MAC地址
     *
     * @param mac MAC地址
     * @return 是否有效
     */
    public static boolean isValidMacAddress(String mac) {
        if (StringUtils.isBlank(mac)) {
            return false;
        }
        return MAC_ADDRESS_PATTERN.matcher(mac).matches();
    }

    /**
     * 验证URL
     *
     * @param url URL地址
     * @return 是否有效
     */
    public static boolean isValidUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        return URL_PATTERN.matcher(url).matches();
    }

    /**
     * 验证用户名
     * 规则：4-20位，字母开头，只能包含字母、数字、下划线
     *
     * @param username 用户名
     * @return 是否有效
     */
    public static boolean isValidUsername(String username) {
        if (StringUtils.isBlank(username)) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * 验证密码强度
     * 规则：8-32位，必须包含大小写字母和数字
     *
     * @param password 密码
     * @return 是否符合要求
     */
    public static boolean isValidPassword(String password) {
        if (StringUtils.isBlank(password)) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * 验证是否为纯中文
     *
     * @param text 文本
     * @return 是否纯中文
     */
    public static boolean isChineseOnly(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }
        return CHINESE_PATTERN.matcher(text).matches();
    }

    /**
     * 验证中文姓名
     * 规则：2-10个中文字符
     *
     * @param name 姓名
     * @return 是否有效
     */
    public static boolean isValidChineseName(String name) {
        if (StringUtils.isBlank(name)) {
            return false;
        }
        return CHINESE_NAME_PATTERN.matcher(name).matches();
    }

    /**
     * 验证字符串长度
     *
     * @param str 字符串
     * @param min 最小长度
     * @param max 最大长度
     * @return 是否在范围内
     */
    public static boolean isLengthInRange(String str, int min, int max) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        return length >= min && length <= max;
    }

    /**
     * 验证数值范围
     *
     * @param value 数值
     * @param min   最小值
     * @param max   最大值
     * @return 是否在范围内
     */
    public static boolean isNumberInRange(long value, long min, long max) {
        return value >= min && value <= max;
    }

    /**
     * 验证字符串是否为空
     *
     * @param str 字符串
     * @return 是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 验证字符串是否不为空
     *
     * @param str 字符串
     * @return 是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 验证对象是否为空
     *
     * @param obj 对象
     * @return 是否为空
     */
    public static boolean isNull(Object obj) {
        return obj == null;
    }

    /**
     * 验证对象是否不为空
     *
     * @param obj 对象
     * @return 是否不为空
     */
    public static boolean isNotNull(Object obj) {
        return obj != null;
    }

    /**
     * 脱敏手机号
     * 例如：13812345678 -> 138****5678
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (!isValidPhone(phone)) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 脱敏邮箱
     * 例如：test@example.com -> t***@example.com
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (!isValidEmail(email)) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        String prefix = email.substring(0, 1) + "***";
        return prefix + email.substring(atIndex);
    }

    /**
     * 脱敏身份证号
     * 例如：110101199001011234 -> 110101********1234
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String idCard) {
        if (!isValidIdCard(idCard)) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }

    /**
     * 脱敏姓名
     * 例如：张三 -> 张*，张三丰 -> 张**
     *
     * @param name 姓名
     * @return 脱敏后的姓名
     */
    public static String maskChineseName(String name) {
        if (StringUtils.isBlank(name)) {
            return name;
        }
        if (name.length() <= 1) {
            return name;
        }
        return name.charAt(0) + StringUtils.repeat("*", name.length() - 1);
    }

    // ========== 业务规则校验 ==========

    /**
     * 校验业务规则
     *
     * @param condition 条件表达式
     * @param message 错误消息
     * @throws com.haven.base.common.exception.BusinessException 业务异常
     */
    public static void validateBusinessRule(boolean condition, String message) {
        if (!condition) {
            throw new com.haven.base.common.exception.BusinessException(
                com.haven.base.common.response.ErrorCode.BUSINESS_ERROR, message);
        }
    }

    /**
     * 校验业务规则（自定义错误码）
     *
     * @param condition 条件表达式
     * @param errorCode 错误码
     * @param message 错误消息
     */
    public static void validateBusinessRule(boolean condition,
                                          com.haven.base.common.response.ErrorCode errorCode,
                                          String message) {
        if (!condition) {
            throw new com.haven.base.common.exception.BusinessException(errorCode, message);
        }
    }

    /**
     * 校验参数不为空
     *
     * @param value 参数值
     * @param paramName 参数名称
     */
    public static void requireNonNull(Object value, String paramName) {
        if (value == null) {
            throw new com.haven.base.common.exception.ValidationException(
                paramName, paramName + "不能为空");
        }
    }

    /**
     * 校验字符串不为空
     *
     * @param value 字符串值
     * @param paramName 参数名称
     */
    public static void requireNonBlank(String value, String paramName) {
        if (StringUtils.isBlank(value)) {
            throw new com.haven.base.common.exception.ValidationException(
                paramName, paramName + "不能为空");
        }
    }

    /**
     * 校验数值范围
     *
     * @param value 数值
     * @param min 最小值
     * @param max 最大值
     * @param paramName 参数名称
     */
    public static void requireInRange(long value, long min, long max, String paramName) {
        if (value < min || value > max) {
            throw new com.haven.base.common.exception.ValidationException(
                paramName, String.format("%s必须在%d到%d之间", paramName, min, max));
        }
    }

    /**
     * 校验集合不为空
     *
     * @param collection 集合
     * @param paramName 参数名称
     */
    public static void requireNonEmpty(java.util.Collection<?> collection, String paramName) {
        if (collection == null || collection.isEmpty()) {
            throw new com.haven.base.common.exception.ValidationException(
                paramName, paramName + "不能为空");
        }
    }

    /**
     * 校验字符串长度
     *
     * @param value 字符串值
     * @param minLength 最小长度
     * @param maxLength 最大长度
     * @param paramName 参数名称
     */
    public static void requireLength(String value, int minLength, int maxLength, String paramName) {
        if (value == null) {
            throw new com.haven.base.common.exception.ValidationException(
                paramName, paramName + "不能为空");
        }
        int length = value.length();
        if (length < minLength || length > maxLength) {
            throw new com.haven.base.common.exception.ValidationException(
                paramName, String.format("%s长度必须在%d到%d之间", paramName, minLength, maxLength));
        }
    }

    /**
     * 校验正则表达式
     *
     * @param value 字符串值
     * @param pattern 正则表达式
     * @param paramName 参数名称
     * @param message 错误消息
     */
    public static void requirePattern(String value, String pattern, String paramName, String message) {
        if (StringUtils.isBlank(value) || !value.matches(pattern)) {
            throw new com.haven.base.common.exception.ValidationException(paramName, message);
        }
    }
}