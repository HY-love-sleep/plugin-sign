package com.cubigdata.plugin.sign.common.constant;

/**
 * Description: 签名涉及的redisKey 常量
 *
 * @author: yhong
 * Date: 2024/12/9
 */
public interface SignConstant {
    String SYS_SIGN_CACHE_KEY = "EXPOS:ADMIN:SYS_SIGN_CACHE_KEY:";
    String SIGN_INFO = "sign_info";
    String DICT_GROUP = "SYS_DICT_GROUP";

    String APP_KEY= "appKey";
    String APP_SECRET = "appSecret";

    String TIMESTAMP = "timestamp";
    String NONCE = "nonce";
    String SIGNATURE = "sign";

}
