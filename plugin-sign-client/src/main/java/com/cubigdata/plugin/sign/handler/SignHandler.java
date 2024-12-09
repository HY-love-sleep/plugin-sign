package com.cubigdata.plugin.sign.handler;

import com.cubigdata.expos.framework.core.wrapper.BufferedHttpRequestWrapper;
import com.cubigdata.plugin.sign.common.dto.SignDTO;

/**
 * 签名处理器
 */
public interface SignHandler {
    /**
     * 处理请求、校验签名
     * @param request
     * @param signInfo
     * @return
     */
    boolean handle(BufferedHttpRequestWrapper request, SignDTO signInfo);
}
