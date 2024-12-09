package com.cubigdata.plugin.sign.autoconfigure;

import com.cubigdata.plugin.sign.common.properties.SignProperties;
import com.cubigdata.plugin.sign.filter.SignFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Description: 签名自动装配类
 *
 * @author: yhong
 * Date: 2024/12/9
 */
@Configuration
@EnableConfigurationProperties(SignProperties.class)
@ConditionalOnClass(RedisTemplate.class)
@ComponentScan(basePackages = "com.cubigdata.plugin.sign.handler")
public class SignAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public SignFilter signFilter() {
        return new SignFilter();
    }

    @Bean
    public SignCacheInitializer signCacheInitializer() {
        return new SignCacheInitializer();
    }
}
