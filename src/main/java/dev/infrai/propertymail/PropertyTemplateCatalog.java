package dev.infrai.propertymail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

@Component
public final class PropertyTemplateCatalog {
    private final PropertyMailConfig config;
    private final Clock clock;

    @Autowired
    public PropertyTemplateCatalog(PropertyMailConfig config) {
        this(config, Clock.systemUTC());
    }

    PropertyTemplateCatalog(PropertyMailConfig config, Clock clock) {
        this.config = config;
        this.clock = clock;
    }

    public RenderedMail render(LifecycleMail mail) {
        String greeting = "<p>Hello " + escape(mail.tenantName()) + ",</p>";
        String property = escape(mail.propertyName());
        String reference = escape(mail.reference());
        return switch (mail.kind()) {
            case MAINTENANCE_REQUEST -> new RenderedMail(
                    "Maintenance request received: " + mail.reference(),
                    greeting + "<p>We received maintenance request <strong>" + reference
                            + "</strong> for " + property + ".</p>");
            case TENANT_DOCUMENT -> new RenderedMail(
                    "Tenant document available: " + mail.reference(),
                    greeting + "<p>Your document <strong>" + reference
                            + "</strong> is available for " + property + ".</p>");
            case INSPECTION_REMINDER -> {
                long days = ChronoUnit.DAYS.between(clock.instant().atZone(java.time.ZoneOffset.UTC).toLocalDate(), mail.dueDate());
                if (days < 0 || days > config.reminderWindowDays()) {
                    throw new MailNotDueException("Inspection reminder is outside the configured delivery window");
                }
                yield new RenderedMail(
                        "Inspection reminder for " + mail.propertyName(),
                        greeting + "<p>Your inspection for " + property + " is scheduled on <strong>"
                                + mail.dueDate() + "</strong>. Reference: " + reference + ".</p>");
            }
        };
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    public record RenderedMail(String subject, String html) {}
    public static final class MailNotDueException extends RuntimeException {
        public MailNotDueException(String message) { super(message); }
    }
}
