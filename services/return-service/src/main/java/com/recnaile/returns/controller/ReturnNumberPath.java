package com.recnaile.returns.controller;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public final class ReturnNumberPath {
    private ReturnNumberPath(){}
    public static String normalize(String value){
        if(value==null||value.isBlank()) return value;
        String normalized=value;
        if(normalized.contains("%25")) {
            try { normalized=URLDecoder.decode(normalized, StandardCharsets.UTF_8); } catch(IllegalArgumentException ignored) {}
        }
        return normalized;
    }
}
