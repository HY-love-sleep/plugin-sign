package com.cubigdata.plugin.sign.feign;

import com.cubigdata.plugin.sign.common.dto.SignDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "signHttpApi",url = "${expos.http.url}")
public interface SignHttp {

    @GetMapping("/sign/appKey")
    SignDTO getSignInfoByAppKey(@RequestParam("appKey") String appKey);
}