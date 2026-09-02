package dev.infrai.propertymail;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("property-mail")
public record PropertyMailConfig(int reminderWindowDays, String infraiBaseUrl) {}
