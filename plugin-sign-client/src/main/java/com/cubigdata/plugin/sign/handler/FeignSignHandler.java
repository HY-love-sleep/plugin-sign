package com.cubigdata.plugin.sign.handler;

import com.cubigdata.expos.framework.core.wrapper.BufferedHttpRequestWrapper;
import com.cubigdata.plugin.sign.common.constant.SignConstant;
import com.cubigdata.plugin.sign.common.dto.SignDTO;
import com.cubigdata.plugin.sign.common.enums.SignTypeEnum;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Description: 验证feign请求中的签名
 * 目的是兼容expos-components中FeignClientSignInterceptor对feign调用添加的签名逻辑
 * @author: yhong
 * Date: 2024/10/18
 */
@Slf4j
@Component
public class FeignSignHandler implements SignHandler, InitializingBean {

    @Override
    public boolean handle(BufferedHttpRequestWrapper request, SignDTO signInfo) {
        // FeignClientSignInterceptor中添加的sign
        String headerSign = request.getHeader(SignConstant.SIGNATURE);

        // 按FeignClientSignInterceptor中同样的逻辑计算sign
        // todo: 没有加上请求参数， 有需要再修改
        String appSecret = signInfo.getAppSecret();
        Map<String, String> params = Maps.newHashMap();
        params.put("path", request.getHeader("path"));
        params.put("timestamp", request.getHeader("timestamp"));
        String calSign = generateSign(appSecret, params);
        return StringUtils.equals(headerSign, calSign);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        HandlerFactory.register(SignTypeEnum.FEIGN.getType(), this);
    }

    private String generateSign(String signKey, Map<String, String> params) {
        List<String> storedKeys = Arrays.stream(params.keySet().toArray(new String[0])).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        String sign = (storedKeys.stream().filter((key) -> {
            return !Objects.equals(key, "sign");
        }).map((key) -> {
            return String.join("", key, (CharSequence)params.get(key));
        }).collect(Collectors.joining())).trim().concat(signKey);
        return DigestUtils.md5DigestAsHex(sign.getBytes()).toUpperCase();
    }
}
