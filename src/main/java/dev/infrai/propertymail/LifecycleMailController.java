package dev.infrai.propertymail;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public final class LifecycleMailController {
    private final PropertyTemplateCatalog templates;
    private final InfraiEmailClient email;

    public LifecycleMailController(PropertyTemplateCatalog templates, InfraiEmailClient email) {
        this.templates = templates;
        this.email = email;
    }

    @PostMapping("/property-mails")
    public InfraiEmailClient.SendResult send(@RequestBody LifecycleMail request) {
        return email.send(request.tenantEmail(), templates.render(request), request.eventId());
    }

    @ExceptionHandler(PropertyTemplateCatalog.MailNotDueException.class)
    ResponseEntity<Map<String, String>> notDue(PropertyTemplateCatalog.MailNotDueException error) {
        return ResponseEntity.unprocessableEntity().body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler(InfraiEmailClient.InfraiException.class)
    ResponseEntity<Map<String, String>> rejected(InfraiEmailClient.InfraiException error) {
        int status = error.status() >= 400 && error.status() < 500 ? error.status() : 502;
        return ResponseEntity.status(status).body(Map.of("code", error.code(), "error", error.getMessage()));
    }
}
