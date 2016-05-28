package com.vaadin.tests.components.textfield;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.tests.components.TestBase;
import com.vaadin.tests.util.Log;
import com.vaadin.ui.TextField;

public class TextFieldFocusAndBlurListeners extends TestBase implements
        ValueChangeListener {
    private Log log = new Log(5).setNumberLogRows(false);

    @Override
    protected String getDescription() {
        return "Tests the focus and blur functionality of TextField";
    }

    @Override
    protected Integer getTicketNumber() {
        return 3544;
    }

    @Override
    public void setup() {
        addComponent(log);
        TextField tf1 = new TextField("TextField 1",
                "Has focus and blur listeners");
        tf1.setWidth("300px");
        tf1.addFocusListener(this::focus);
        tf1.addBlurListener(this::blur);

        addComponent(tf1);

        TextField tf2 = new TextField("TextField 2",
                "Has focus, blur and valuechange listeners");
        tf2.setWidth("300px");
        tf2.addValueChangeListener(new ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
                TextFieldFocusAndBlurListeners.this.valueChange(event);
            }
        });
        tf2.addFocusListener(new FocusListener() {

            @Override
            public void focus(FocusEvent event) {
                TextFieldFocusAndBlurListeners.this.focus(event);
            }

        });
        tf2.addBlurListener(new BlurListener() {

            @Override
            public void blur(BlurEvent event) {
                TextFieldFocusAndBlurListeners.this.blur(event);
            }
        });

        addComponent(tf2);

        TextField tf3 = new TextField("TextField 3",
                "Has non-immediate valuechange listener");
        tf3.setWidth("300px");
        tf3.addValueChangeListener(this);

        addComponent(tf3);

        TextField tf4 = new TextField("TextField 4",
                "Has immediate valuechange listener");
        tf4.setWidth("300px");
        tf4.setImmediate(true);
        tf4.addValueChangeListener(this);

        addComponent(tf4);
    }

    public void focus(FocusEvent event) {
        log.log(event.getComponent().getCaption() + ": Focus");

    }

    public void blur(BlurEvent event) {
        TextField tf = (TextField) event.getComponent();
        log.log(tf.getCaption() + ": Blur. Value is: "
                + tf.getValue().toString());

    }

    @Override
    public void valueChange(ValueChangeEvent event) {
        TextField tf = (TextField) event.getProperty();
        log.log(tf.getCaption() + ": ValueChange: " + tf.getValue().toString());
    }
}
