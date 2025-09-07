package dev.lvstrng.argon.event.events;

import dev.lvstrng.argon.event.Event;
import dev.lvstrng.argon.event.Listener;

import java.util.ArrayList;

public interface ButtonListener extends Listener {
    void onButtonPress(ButtonEvent event);

    class ButtonEvent extends Event<ButtonListener> {
        public int button, action, scancode;
        public long window;

        public ButtonEvent(int button, long window, int action, int scancode) {
            this.button = button;
            this.window = window;
            this.action = action;
            this.scancode = scancode;
        }

        @Override
        public void fire(ArrayList<ButtonListener> listeners) {
            listeners.forEach(e -> e.onButtonPress(this));
        }

        @Override
        public Class<ButtonListener> getListenerType() {
            return ButtonListener.class;
        }
    }
}
