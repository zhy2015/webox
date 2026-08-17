package com.webox.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String businessTimeZone, String frontendOrigin) {
    public ZoneId zoneId() {
        return ZoneId.of(businessTimeZone);
    }
}
