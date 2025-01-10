package com.hmdp.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @Title: AliOssProperties
 * @Author mingliu0608
 * @Package com.hmdp.properties
 * @Date 2025/1/9 22:46
 * @description: alioss upload properties
 */


@Component
@ConfigurationProperties(prefix = "alioss")
@Data
public class AliOssProperties {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
}
