package com.haven.storage.service.base;

import com.haven.base.common.exception.BaseException;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.common.response.ResponseWrapper;
import com.haven.base.utils.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 存储服务基础类
 * <p>
 * 🎯 核心功能：
 * - 统一异常处理和错误码映射
 * - 标准化日志记录和链路追踪
 * - 提供通用的服务层基础设施能力
 * - 确保所有Service层的异常处理一致性
 * <p>
 * 💡 设计原则：
 * - DRY原则：避免重复的异常处理代码
 * - 松耦合设计：不绑定特定业务参数
 * - 开闭原则：对扩展开放，对修改封闭
 * - 依赖倒置：依赖抽象而非具体实现
 * <p>
 * 📋 使用规范：
 * - 所有Service类都应该继承BaseService
 * - 使用handleException方法处理异常
 * - 遵循统一的错误码和日志格式
 * - 在业务方法中使用@TraceLog注解
 *
 * @author HavenButler
 */
@Slf4j
public abstract class BaseService {

}