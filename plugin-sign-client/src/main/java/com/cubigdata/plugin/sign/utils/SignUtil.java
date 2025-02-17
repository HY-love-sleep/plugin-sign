package com.cubigdata.plugin.sign.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONNull;
import cn.hutool.json.JSONUtil;
import com.cubigdata.expos.framework.core.util.JacksonUtil;
import com.cubigdata.expos.framework.nacos.NacosConfigHelper;
import com.cubigdata.plugin.sign.common.constant.SignConstant;
import com.cubigdata.plugin.sign.common.dto.SignDTO;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * http 工具类 获取请求中的参数
 *
 * @author: yhong
 * Date: 2024/10/18
 */
@Slf4j
public class SignUtil {


    public static SortedMap<String, Object> getAllParams(HttpServletRequest request) throws IOException, ServletException {
        SortedMap<String, Object> result = new TreeMap<>();
        result.put(SignConstant.APP_KEY, decodeValue(request.getHeader("appKey")));
        if (null != request.getHeader(SignConstant.NONCE)) {
            result.put(SignConstant.NONCE, request.getHeader(SignConstant.NONCE));
        }
        String timestamp = request.getHeader("timestamp");
        if (StringUtils.isEmpty(timestamp)) {
            throw new RuntimeException("timestamp 不能为空");
        }
        long parseLong = Long.parseLong(timestamp);
        if (System.currentTimeMillis() - parseLong > 300000L) {
            log.error("请求时间超过五分钟， 拒绝请求");
            throw new RuntimeException("请求超过5分钟， 不进行处理");
        }
        result.put("timestamp", decodeValue(timestamp));
        Map<String, String> urlParams = getUrlParams(request);
        if (CollectionUtils.isNotEmpty(urlParams.entrySet())) {
            Map<String, String> filteredUrlParams = (Map<String, String>) urlParams.entrySet().stream().filter(entry -> (entry.getValue() != null)).collect(Collectors.toMap(Map.Entry::getKey, entry -> decodeValue((String) entry.getValue())));
            result.putAll(flattenParams(filteredUrlParams, ""));
        }
        // 检查请求的内容类型是否为 multipart/form-data
        String contentType = request.getContentType();
        if (contentType != null && contentType.startsWith("multipart/form-data")) {
            // 处理 multipart/form-data 请求
            for (Part part : request.getParts()) {
                if (part.getContentType() != null) {
                    log.info("忽略文件参数: {}", part.getName());
                } else {
                    // 将普通参数添加到结果中
                    String paramName = part.getName();
                    String paramValue = request.getParameter(paramName);
                    if (paramValue != null) {
                        result.put(paramName, decodeValue(paramValue));
                    }
                }
            }
        } else {
            // 正常处理非 multipart/form-data 请求
            if (!HttpMethod.GET.name().equals(request.getMethod())) {
                Map<String, String> allRequestParam = getAllRequestParam(request);
                if (allRequestParam != null) {
                    result.putAll(flattenParams(allRequestParam, ""));
                }
            }
        }
        return result;
    }

    private static SortedMap<String, Object> flattenParams(Map<String, ?> params, String prefix) {
        SortedMap<String, Object> result = new TreeMap<>();
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : (prefix + "." + (String)entry.getKey());
            Object value = entry.getValue();
            if (value == null || value instanceof JSONNull) {
                result.put(key, "null");
                continue;
            }
            if (value instanceof Map) {
                Map<String, ?> mapValue = (Map<String, ?>)value;
                if (prefix.isEmpty()) {
                    result.put(key, jsonEncodeInternal(mapValue));
                    continue;
                }
                result.putAll(flattenParams(mapValue, key));
                continue;
            }
            if (value instanceof Iterable) {
                result.put(key, jsonEncode(value));
                continue;
            }
            result.put(key, jsonEncode(value));
        }
        return result;
    }

    private static String jsonEncode(Object value) {
        if (value instanceof String) {
            try {
                return (String)value;
            } catch (Exception e) {
                throw new RuntimeException("Failed to encode value: " + value, e);
            }
        }
        return jsonEncodeInternal(value);
    }

    private static String jsonEncodeInternal(Object value) {
        if (value == null || value instanceof JSONNull) {
            return "null";
        }
        if (value instanceof Iterable) {
            List<Object> sortedList = new ArrayList<>();
            for (Object item : (Iterable<?>) value) {
                if (item instanceof Map) {
                    sortedList.add(sortMap((Map<String, ?>)item));
                    continue;
                }
                sortedList.add(item);
            }
            return (new Gson()).toJson(sortedList);
        }
        if (value instanceof Map) {
            return (new Gson()).toJson(sortMap((Map<String, ?>)value));
        }
        return (new Gson()).toJson(value);
    }

    private static Map<String, Object> sortMap(Map<String, ?> map) {
        Map<String, Object> sortedMap = new TreeMap<>();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value == null || value instanceof JSONNull) {
                sortedMap.put(entry.getKey(), "null");
                continue;
            }
            sortedMap.put(entry.getKey(), value);
        }
        return sortedMap;
    }

    public static Map<String, String> getAllRequestParam(HttpServletRequest request) throws IOException {
        String contentLength = request.getHeader("Content-Length");
        String contentType = request.getHeader("Content-Type");
        if (contentLength == null || "0".equals(contentLength) || !contentType.startsWith("application/json")) {
            return Collections.emptyMap();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream)request.getInputStream()));
        String str = "";
        StringBuilder wholeStr = new StringBuilder();
        while ((str = reader.readLine()) != null) {
            wholeStr.append(decodeValue(str));
        }
        String requestBody = wholeStr.toString();

        // 检查请求体是否为 JSON 数组
        if (requestBody.startsWith("[") && requestBody.endsWith("]")) {
            // 请求体是 JSON 数组
            Map<String, String> resultMap = new HashMap<>();
            resultMap.put("array", requestBody);
            return resultMap;
        } else {
            // 请求体是 JSON 对象
            Map<String, Object> objectMap = JSONUtil.toBean(requestBody, Map.class);
            Map<String, String> stringMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
                stringMap.put(entry.getKey(), entry.getValue().toString());
            }
            return stringMap;
        }
    }

    public static Map<String, String> getUrlParams(HttpServletRequest request) {
        String param = "";
        String queryString = request.getQueryString();
        if (queryString == null) {
            return new HashMap<>();
        }
        try {
            param = URLDecoder.decode(queryString, "utf-8");
        } catch (UnsupportedEncodingException e) {
            log.error("{}", request.getRequestURI());
        }
        Map<String, String> result = new HashMap<>();
        String[] params = param.split("&");
        for (String s : params) {
            int index = s.indexOf("=");
            if (index == -1) {
                result.put(s, "");
            } else {
                result.put(s.substring(0, index), decodeValue(s.substring(index + 1)));
            }
        }
        return result;
    }

    public static String buildSignString(SortedMap<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                sb.append(key).append("=").append("null").append("&");
                continue;
            }
            if (value instanceof Map) {
                sb.append(key).append("=").append(jsonEncodeInternal(value)).append("&");
                continue;
            }
            sb.append(key).append("=").append(value).append("&");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private static String decodeValue(String value) {
        if (value == null) {
            return null;
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            log.error("{}", value, e);
            return value;
        }
    }
}