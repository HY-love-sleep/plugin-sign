package com.cubigdata.plugin.sign.autoconfigure;

import com.cubigdata.expos.framework.nacos.NacosConfigHelper;
import com.cubigdata.plugin.sign.common.properties.SignProperties;
import com.cubigdata.plugin.sign.feign.SignHttp;
import com.cubigdata.plugin.sign.filter.SignFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.Resource;

/**
 * Description: 签名自动装配类
 *
 * @author: yhong
 * Date: 2024/12/9
 */
@Configuration
@EnableConfigurationProperties(SignProperties.class)
@ComponentScan(basePackages = "com.cubigdata.plugin.sign")
@EnableFeignClients(basePackages = "com.cubigdata.plugin.sign.feign")
@Slf4j
@AutoConfigureBefore(SignFilter.class)
public class SignConfig implements InitializingBean {
    @Resource(name = "signRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private SignHttp signHttp;
    @Autowired
    private SignProperties signProperties;
    @Autowired
    private WebApplicationContext applicationContext;
    @Autowired
    private ServerProperties serverProperties;
    @Autowired
    private NacosConfigHelper nacosConfigUtil;

    @Bean
    @ConditionalOnProperty(prefix = "sign", name = "enabled", havingValue = "true")
    public SignFilter signFilter() {
        return new SignFilter(redisTemplate, signHttp, signProperties, serverProperties, applicationContext, nacosConfigUtil);
    }

    @Bean(name = "signRedisTemplate")
    @Lazy
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory();
    }


    @Bean
    public SignCacheInitializer signCacheInitializer() {
        return new SignCacheInitializer();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("sign client init success ");
    }
}
