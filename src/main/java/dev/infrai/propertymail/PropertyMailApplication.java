package dev.infrai.propertymail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PropertyMailApplication {
    public static void main(String[] args) {
        SpringApplication.run(PropertyMailApplication.class, args);
    }
}
