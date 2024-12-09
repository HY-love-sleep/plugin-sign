package com.cubigdata.plugin.sign.filter;

import cn.hutool.json.JSONUtil;
import com.cubigdata.expos.framework.core.response.BaseResult;
import com.cubigdata.expos.framework.core.wrapper.BufferedHttpRequestWrapper;
import com.cubigdata.expos.framework.utils.ResponseWriter;
import com.cubigdata.expos.framework.utils.UriMatcher;
import com.cubigdata.plugin.sign.annotation.Appoint;
import com.cubigdata.plugin.sign.common.constant.SignConstant;
import com.cubigdata.plugin.sign.common.dto.SignDTO;
import com.cubigdata.plugin.sign.common.properties.SignProperties;
import com.cubigdata.plugin.sign.feign.SignHttp;
import com.cubigdata.plugin.sign.handler.HandlerFactory;
import com.cubigdata.plugin.sign.handler.SignHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Order(1)
public class SignFilter extends OncePerRequestFilter {

    @Resource
    private SignHttp signHttp;
    @Resource
    private SignProperties signProperties;
    @Autowired(required = false)
    private ServerProperties serverProperties;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private WebApplicationContext applicationContext;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!signProperties.getEnabled()) {
            log.debug("sign filter is not enabled");
            return true;
        }
        if (CollectionUtils.isEmpty(signProperties.getUris())) {
            log.debug("sign uri not configured");
            return true;
        }
        String requestURI = request.getRequestURI();
        String contextPath = null;
        if (null != serverProperties) {
            contextPath = serverProperties.getServlet().getContextPath();
        }
        Boolean match = UriMatcher.match(contextPath, requestURI, signProperties.getUris());
        if (match) {
            log.debug("该接口设置为不经过签名校验, pass :{}", requestURI);
            return true;
        }
        // 兼容common-components TokenFilter中的签名逻辑
        if (isAppointAnnotated(request)) {
            log.debug("该接口被 @Appoint 注解修饰, sign pass :{}", requestURI);
            return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain) throws ServletException, IOException {

        BaseResult result = new BaseResult();
        result.setCode(-9900);
        result.setMsg("报文签名解析失败");
        result.setData(null);

        String appKey = request.getHeader(SignConstant.APP_KEY);
        if(StringUtils.isEmpty(appKey)){
            ResponseWriter.write(response, JSONUtil.toJsonStr(result));
            return;
        }

        BufferedHttpRequestWrapper requestWrapper = new BufferedHttpRequestWrapper(request);
        SignDTO signInfo;
        try {
            signInfo = (SignDTO) redisTemplate.opsForValue().get(SignConstant.SYS_SIGN_CACHE_KEY + appKey);
            if (null == signInfo) {
                signInfo = signHttp.getSignInfoByAppKey(appKey);
                if (null == signInfo) {
                    log.error("无法获取签名验证信息");
                    // 没有颁发过这个appKey
                    ResponseWriter.write(response, JSONUtil.toJsonStr(result));
                    return;
                } else {
                    redisTemplate.opsForValue().set(SignConstant.SYS_SIGN_CACHE_KEY + appKey, signInfo);
                }
            }
        } catch (Exception e) {
            log.error("getting sign info error:{}", e.getMessage(), e);
            ResponseWriter.write(response, JSONUtil.toJsonStr(result));
            return;
        }

        SignHandler handler = HandlerFactory.getHandler(signInfo.getSignType());
        if (null == handler) {
            log.error("无法获取签名验证处理器");
            ResponseWriter.write(response, JSONUtil.toJsonStr(result));
            return;
        }

        boolean verified = handler.handle(requestWrapper, signInfo);
        if (!verified) {
            log.error("sign 校验失败");
            ResponseWriter.write(response, JSONUtil.toJsonStr(result));
            return;
        }
        filterChain.doFilter(requestWrapper, response);
    }

    private boolean isAppointAnnotated(HttpServletRequest request) {
        AbstractHandlerMapping handlerMapping = applicationContext.getBean(RequestMappingInfoHandlerMapping.class);
        HandlerExecutionChain handlerChain = null;
        try {
            handlerChain = handlerMapping.getHandler(request);
        } catch (Exception e) {
            log.error("获取处理方法失败", e);
            return false;
        }
        if (handlerChain != null && handlerChain.getHandler() instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handlerChain.getHandler();
            return handlerMethod.getMethodAnnotation(Appoint.class) != null;
        }
        return false;
    }

}
