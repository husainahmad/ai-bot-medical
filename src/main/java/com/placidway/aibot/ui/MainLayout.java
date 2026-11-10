package com.placidway.aibot.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Div;

/**
 * Shared application shell used by all views via {@code layout = MainLayout.class}.
 * <p>
 * Renders an empty navbar and content area; the view provides its own full
 * composition (header, conversation, composer).
 *
 * @author Husain Harmoni
 */
public class MainLayout extends AppLayout {

    /** Creates the shell with an empty navbar and content area. */
    public MainLayout() {
        addToNavbar(new Div());
        setContent(new Div());
    }
}
