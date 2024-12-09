package com.cubigdata.plugin.sign.handler;

import com.google.common.collect.Maps;

import java.util.Map;

public class HandlerFactory {

    private static final Map<String, SignHandler> HANDLER_MAP = Maps.newConcurrentMap();

    public static SignHandler getHandler(String type) {
        return HANDLER_MAP.get(type);
    }

    public static void register(String type, SignHandler signHandler) {
        HANDLER_MAP.put(type, signHandler);
    }

}
