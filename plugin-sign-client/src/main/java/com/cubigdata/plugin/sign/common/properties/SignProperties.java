package com.cubigdata.plugin.sign.common.properties;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "sign")
public class SignProperties {
    // 默认不开启nonce校验
    private Boolean nonce = false;
    // 默认不校验签名
    private Boolean enabled = false;
    private List<String> uris = Lists.newArrayList();
    private List<String> signUris = Lists.newArrayList();
}
