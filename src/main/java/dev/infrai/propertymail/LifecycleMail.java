package dev.infrai.propertymail;

import java.time.LocalDate;

public record LifecycleMail(
        String eventId,
        Kind kind,
        String tenantEmail,
        String tenantName,
        String propertyName,
        String reference,
        LocalDate dueDate) {
    public enum Kind { MAINTENANCE_REQUEST, TENANT_DOCUMENT, INSPECTION_REMINDER }
}
