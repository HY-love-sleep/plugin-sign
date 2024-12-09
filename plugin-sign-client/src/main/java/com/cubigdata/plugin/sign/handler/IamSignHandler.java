package com.cubigdata.plugin.sign.handler;

import cn.hutool.json.JSONUtil;
import com.cubigdata.expos.framework.core.wrapper.BufferedHttpRequestWrapper;
import com.cubigdata.plugin.sign.common.dto.SignDTO;
import com.cubigdata.plugin.sign.common.enums.SignTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Component
public class IamSignHandler implements SignHandler, InitializingBean {
    private static final String HAINAN_CLIENT_APPID = "hainan-client-appid";
    private static final String APP_KEY = "client_credentials";
    private static final String APP_SECRET = "client_credentials";
    private static final String HAINAN_CLIENT_TIMESTAMP = "hainan-client-timestamp";
    private static final String HAINAN_CLIENT_NONCE = "hainan-client-nonce";
    private static final String HAINAN_CLIENT_SIGNATURE = "hainan-client-signature";

    @Override
    public boolean handle(BufferedHttpRequestWrapper request, SignDTO signInfo) {

        try {
            Map<String, Object> params = new TreeMap<>();
            params.put(HAINAN_CLIENT_APPID, APP_KEY);
            params.put(HAINAN_CLIENT_NONCE, request.getHeader(HAINAN_CLIENT_NONCE));
            params.put(HAINAN_CLIENT_TIMESTAMP, request.getHeader(HAINAN_CLIENT_TIMESTAMP));
            log.debug("rr-" + JSONUtil.toJsonStr(params));

            String inSign = buildSignature(params, APP_SECRET);
            log.debug("-------inSign:{}", inSign);
            String signature = request.getHeader(HAINAN_CLIENT_SIGNATURE);
            log.debug("----signature:{}", signature);
            if (!inSign.equals(signature)) {
                log.error("header to session error-2");
                return false;
            }
        } catch (Exception e) {
            log.error("header to session error-1");
            return false;
        }
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        HandlerFactory.register(SignTypeEnum.IAM.getType(),this);
    }

    private String buildSignature(Map<String, Object> maps, String appSecret) {
        String s2;
        try {
            //header+param拼凑
            String sbr = JSONUtil.toJsonStr(maps) + "#" + appSecret;

            String str = sbr.replace("\"", "").replace("{", "").replace("}", "");
            System.out.println("ss-" + str);
            s2 = DigestUtils.md5DigestAsHex(str.getBytes()).toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("map转json异常", e);
        }
        return s2;
    }
}
