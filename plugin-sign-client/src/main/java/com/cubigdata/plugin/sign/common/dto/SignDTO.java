package com.cubigdata.plugin.sign.common.dto;


import lombok.*;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SignDTO implements Serializable {
    private static final long serialVersionUID = -2369091980732112768L;

    /**
     * 客户端标识
     */
    private String appKey;
    /**
     * 签名秘钥
     */
    private String appSecret;
    /**
     * 签名类型，决定了使用哪个handler
     */
    private String signType;



}
