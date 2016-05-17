package com.vaadin.tests.react.eventbus;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.react.React;
import com.vaadin.tests.components.AbstractTestUI;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;

public class EventBusTestUI extends AbstractTestUI {

    @Override
    protected void setup(VaadinRequest request) {

        Button b = new Button("Click me!");
        Label old = new Label();
        Label neu = new Label();

        addComponents(b, old, neu);

        b.addClickListener(e -> old.setValue(e.getClientX() + ","
                + e.getClientY()));

        React.bind(neu::setValue,
                b.clicks().map(e -> e.getClientX() + "," + e.getClientY()));
    }

}
