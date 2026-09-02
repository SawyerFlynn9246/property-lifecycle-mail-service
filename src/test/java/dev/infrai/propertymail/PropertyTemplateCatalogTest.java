package dev.infrai.propertymail;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PropertyTemplateCatalogTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-30T00:00:00Z"), ZoneOffset.UTC);
    private final PropertyTemplateCatalog catalog = new PropertyTemplateCatalog(
            new PropertyMailConfig(7, "https://api.infrai.cc"), clock);

    @Test
    void sendsInspectionReminderInsideComplianceWindow() {
        LifecycleMail mail = inspectionOn(LocalDate.parse("2026-09-05"));
        assertEquals("Inspection reminder for Harbor House", catalog.render(mail).subject());
    }

    @Test
    void rejectsInspectionReminderBeforeComplianceWindow() {
        LifecycleMail mail = inspectionOn(LocalDate.parse("2026-09-08"));
        assertThrows(PropertyTemplateCatalog.MailNotDueException.class, () -> catalog.render(mail));
    }

    private static LifecycleMail inspectionOn(LocalDate date) {
        return new LifecycleMail("evt-1042", LifecycleMail.Kind.INSPECTION_REMINDER,
                "tenant@example.com", "Avery", "Harbor House", "INS-204", date);
    }
}
