package fr.braindead.websocket.util;

/**
 *
 * Created by leiko on 2/17/17.
 */
public class Counter {
    private int value;

    public Counter() {
        this.value = 0;
    }

    public void inc() {
        this.value = this.value + 1;
    }

    public void dec() {
        this.value = this.value - 1;
    }

    public int getValue() {
        return this.value;
    }
}
