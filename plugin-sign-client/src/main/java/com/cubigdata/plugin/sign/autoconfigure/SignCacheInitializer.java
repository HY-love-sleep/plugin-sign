package com.cubigdata.plugin.sign.autoconfigure;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.cubigdata.expos.framework.core.util.JacksonUtil;
import com.cubigdata.expos.framework.nacos.NacosConfigHelper;
import com.cubigdata.plugin.sign.common.constant.SignConstant;
import com.cubigdata.plugin.sign.common.dto.SignDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * Description: 缓存appKey->appSecurity的关系
 *
 * @author: yhong
 * Date: 2024/10/17
 */
@Slf4j
public class SignCacheInitializer implements CommandLineRunner {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    NacosConfigHelper nacosConfigUtil;
    @Override
    public void run(String... args) throws Exception {
        log.info("===================签名模块开始缓存appKey->appSecurity的关系=========================");
        String signInfoConfigStr = nacosConfigUtil.getConfigStrByCode(SignConstant.SIGN_INFO, SignConstant.DICT_GROUP);

        List list = JacksonUtil.jsonToObj(signInfoConfigStr, List.class);
        if (StringUtils.isEmpty(signInfoConfigStr) || CollUtil.isEmpty(list)) {
            return;
        }
        for (Object sign : list) {
            Map<String, Object> objectMap = BeanUtil.beanToMap(sign);
            String appKey = String.valueOf(objectMap.get(SignConstant.APP_KEY));
            SignDTO signDTO = BeanUtil.toBean(objectMap, SignDTO.class);
            redisTemplate.opsForValue()
                    .set(SignConstant.SYS_SIGN_CACHE_KEY + appKey, signDTO);
        }
        log.info("=============appKey->appSecurity 关系已缓存到 Redis===============");
    }
}
