package com.recnaile.inward.observability;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/** Minimal dependency-free JSON console appender for container logs. */
public class JsonConsoleAppender extends AppenderBase<ILoggingEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void append(ILoggingEvent event) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("timestamp", Instant.ofEpochMilli(event.getTimeStamp()).toString());
        json.put("level", event.getLevel().toString());
        json.put("service", "inward-service");
        json.put("logger", event.getLoggerName());
        json.put("thread", event.getThreadName());
        json.put("traceId", value(event, "traceId"));
        json.put("spanId", value(event, "spanId"));
        json.put("correlationId", value(event, CorrelationIdFilter.MDC_KEY));
        json.put("message", event.getFormattedMessage());
        if (event.getThrowableProxy() != null) {
            json.put("exception", event.getThrowableProxy().getClassName());
        }
        try {
            System.out.println(objectMapper.writeValueAsString(json));
        } catch (Exception ignored) {
            System.out.println(event.getFormattedMessage());
        }
    }

    private String value(ILoggingEvent event, String key) {
        String value = event.getMDCPropertyMap().get(key);
        return value == null ? "" : value;
    }
}
