package com.cubigdata.plugin.sign.handler;

import cn.hutool.json.JSONUtil;
import com.cubigdata.expos.framework.core.exception.BusinessException;
import com.cubigdata.expos.framework.core.wrapper.BufferedHttpRequestWrapper;
import com.cubigdata.expos.framework.nacos.NacosConfigHelper;
import com.cubigdata.plugin.sign.common.constant.SignConstant;
import com.cubigdata.plugin.sign.common.dto.SignDTO;
import com.cubigdata.plugin.sign.common.enums.SignTypeEnum;
import com.cubigdata.plugin.sign.utils.SignUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.SortedMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CommonSignHandler implements SignHandler, InitializingBean {
    @Value("${sign.nonce}")
    private Boolean enableNonce;
    @Resource(name = "signRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private NacosConfigHelper nacosConfigUtil;
    private static final String NONCE_KEY = "SIGN_NONCE_KEY:";
    /**
     * 1. 对除签名外的所有请求参数按key做升序排列，value无需编码。（假设当前时间戳是1251234334，有三个参数，c=1,b=2,a=3,加上时间戳之后，
     * 按key排序为：a=3,b=2,c=1,_timestamp=1251234334）
     * 2. 把参数名和参数值连接成字符串，得到拼装字符：a3b2c1_timestamp1251234334
     * 3. 用申请的appSecret连接到拼装字符串的头部和尾部，然后进行32位MD5加密，最后得到的MD5加密摘要转化为大写。
     */
    @Override
    public boolean handle(BufferedHttpRequestWrapper request, SignDTO signInfo) {
        // 校验nonce
        if (enableNonce) {
            String uuid = request.getHeader(SignConstant.NONCE);
            if (uuid == null || uuid.isEmpty()) {
                log.error("请求未携带nonce, 拒绝请求, {}", request.getRequestURI());
                throw new BusinessException("请求未携带nonce, 拒绝请求");
            }
            Object o = redisTemplate.opsForValue().get(NONCE_KEY + uuid);
            if (null == o) {
                redisTemplate.opsForValue().set(NONCE_KEY + uuid, uuid, Duration.ofMinutes(1));
            } else {
                log.error("该请求已失效，拒绝请求， url:{}, nonce:{}", request.getRequestURI(), uuid);
                return false;
            }
        }

        // 获取全部参数(包括URL和body上的)
        SortedMap<String, Object> allParams = null;
        String signature = null;
        String inSign = null;
        try {
            allParams = SignUtil.getAllParams(request);
            inSign = buildSignature(allParams, signInfo.getAppSecret());
            signature = request.getHeader(SignConstant.SIGNATURE);
            if (inSign.equals(signature)) {
                return true;
            } else {
                log.error("签名校验失败， 请求参数:{}， header sign:{}， server sign:{}", JSONUtil.toJsonStr(allParams), signature, inSign);
                return false;
            }
        } catch (Exception e) {
            log.error("签名校验失败， 请求参数:{}， header sign:{}， server sign:{}", JSONUtil.toJsonStr(allParams), signature, inSign, e);
            return false;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        HandlerFactory.register(SignTypeEnum.COMMON.getType(), this);
    }

    public String buildSignature(SortedMap<String, Object> param, String appSecret) {
        log.info("组装前-" + JSONUtil.toJsonStr(param) + "-" + appSecret);

        // 将所有值转换为字符串
        String trim = param.entrySet().stream()
                .map(entry -> String.join("=", entry.getKey(), convertToString(entry.getValue())))
                .collect(Collectors.joining("&"))
                .trim()
                .concat("&appSecret=" + appSecret);
        trim = trim.replace("\"null\"", "null");
        log.info("签名组装参数字符串: {}", trim);
        return DigestUtils.md5DigestAsHex(trim.getBytes()).toUpperCase();
    }

    private String convertToString(Object value) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Integer) {
            return String.valueOf(value);
        } else if (value instanceof Long) {
            return String.valueOf(value);
        } else if (value instanceof Boolean) {
            return String.valueOf(value);
        } else if (value instanceof Double) {
            return String.valueOf(value);
        } else if (value instanceof Float) {
            return String.valueOf(value);
        } else if (value instanceof Short) {
            return String.valueOf(value);
        } else if (value instanceof Byte) {
            return String.valueOf(value);
        } else {
            return JSONUtil.toJsonStr(value);
        }
    }
}
