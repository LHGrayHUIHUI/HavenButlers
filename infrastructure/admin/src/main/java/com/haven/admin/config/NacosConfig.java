package com.haven.admin.config;

import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * Nacos配置类
 *
 * 提供Nacos相关Bean配置：
 * - ConfigService：配置管理服务
 * - NamingService：服务发现和注册服务
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Slf4j
@Configuration
public class NacosConfig {

    @Autowired
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    @Autowired
    private NacosConfigProperties nacosConfigProperties;

    /**
     * 创建Nacos配置服务Bean
     *
     * @return ConfigService实例
     * @throws NacosException Nacos异常
     */
    @Bean
    public ConfigService configService() throws NacosException {
        Properties properties = new Properties();

        // 设置Nacos服务器地址
        properties.setProperty("serverAddr", nacosConfigProperties.getServerAddr());

        // 设置命名空间
        if (nacosConfigProperties.getNamespace() != null) {
            properties.setProperty("namespace", nacosConfigProperties.getNamespace());
        }

        // 设置用户名和密码（如果配置了的话）
        if (nacosConfigProperties.getUsername() != null) {
            properties.setProperty("username", nacosConfigProperties.getUsername());
        }
        if (nacosConfigProperties.getPassword() != null) {
            properties.setProperty("password", nacosConfigProperties.getPassword());
        }

        // 设置访问密钥（如果配置了的话）
        if (nacosConfigProperties.getAccessKey() != null) {
            properties.setProperty("accessKey", nacosConfigProperties.getAccessKey());
        }
        if (nacosConfigProperties.getSecretKey() != null) {
            properties.setProperty("secretKey", nacosConfigProperties.getSecretKey());
        }

        log.info("创建Nacos ConfigService，服务器地址: {}", nacosConfigProperties.getServerAddr());
        return NacosFactory.createConfigService(properties);
    }

    /**
     * 创建Nacos命名服务Bean
     *
     * @return NamingService实例
     * @throws NacosException Nacos异常
     */
    @Bean
    public NamingService namingService() throws NacosException {
        Properties properties = new Properties();

        // 设置Nacos服务器地址
        properties.setProperty("serverAddr", nacosDiscoveryProperties.getServerAddr());

        // 设置命名空间
        if (nacosDiscoveryProperties.getNamespace() != null) {
            properties.setProperty("namespace", nacosDiscoveryProperties.getNamespace());
        }

        // 设置用户名和密码（如果配置了的话）
        if (nacosDiscoveryProperties.getUsername() != null) {
            properties.setProperty("username", nacosDiscoveryProperties.getUsername());
        }
        if (nacosDiscoveryProperties.getPassword() != null) {
            properties.setProperty("password", nacosDiscoveryProperties.getPassword());
        }

        // 设置访问密钥（如果配置了的话）
        if (nacosDiscoveryProperties.getAccessKey() != null) {
            properties.setProperty("accessKey", nacosDiscoveryProperties.getAccessKey());
        }
        if (nacosDiscoveryProperties.getSecretKey() != null) {
            properties.setProperty("secretKey", nacosDiscoveryProperties.getSecretKey());
        }

        log.info("创建Nacos NamingService，服务器地址: {}", nacosDiscoveryProperties.getServerAddr());
        return NacosFactory.createNamingService(properties);
    }
}