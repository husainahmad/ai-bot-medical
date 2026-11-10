package com.placidway.aibot;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point and Vaadin app shell configuration.
 * <p>
 * Binds the {@code aibot} theme and enables server push so streamed assistant
 * replies can be rendered incrementally in the chat view.
 *
 * @author Husain Harmoni
 */
@SpringBootApplication
@Theme("aibot")
@Push
@PWA(
        name = "Medical AI Assistant",
        shortName = "Medical AI",
        backgroundColor = "#f8fafc",
        themeColor = "#0f766e"
)
public class AibotApplication implements AppShellConfigurator {

    /**
     * Boots the Spring Boot application.
     *
     * @param args command-line arguments passed through to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(AibotApplication.class, args);
    }

}
