package com.hmdp.config;

import com.hmdp.properties.AliOssProperties;
import com.hmdp.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Title: AliOssConfig
 * @Author mingliu0608
 * @Package com.hmdp.config
 * @Date 2025/1/9 23:21
 * @description: alioss configuration
 */

@Slf4j
@Configuration
public class AliOssConfig   {
    @Autowired
    AliOssProperties aliOssProperties;
//    log.info("开始创建阿里云文件上传工具类对象：{}",aliOssProperties);
    @Bean
    @ConditionalOnMissingBean
    AliOssUtil aliOssUtil(){
        return  new AliOssUtil(aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),
                aliOssProperties.getBucketName());
    }
}
