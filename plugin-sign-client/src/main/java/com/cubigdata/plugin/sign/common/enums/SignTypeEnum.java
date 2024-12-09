package com.cubigdata.plugin.sign.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author liuhy
 * @since 2022/1/26
 */
@AllArgsConstructor
@Getter
public enum SignTypeEnum {

    COMMON("common", "通用"),
    IAM("iam", "安全合规部"),
    FEIGN("feign", "服务内部调用")
    ;

    private final String type;
    private final String name;
}
