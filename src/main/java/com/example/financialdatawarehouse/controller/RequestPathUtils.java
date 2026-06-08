package com.example.financialdatawarehouse.controller;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

final class RequestPathUtils {
    private RequestPathUtils() {
    }

    static String trailingPath(HttpServletRequest request, String marker) {
        String uri = request.getRequestURI();
        int index = uri.indexOf(marker);
        if (index < 0) {
            return "";
        }
        String raw = uri.substring(index + marker.length());
        return URLDecoder.decode(raw, StandardCharsets.UTF_8);
    }
}
