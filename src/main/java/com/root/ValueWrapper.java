package com.root;

import lombok.Getter;
import lombok.ToString;

@ToString
class ValueWrapper {
    private final Object value;
    @Getter
    private long lastAccessTime;
    @Getter
    private long accessCounter;

    public ValueWrapper(Object value) {
        this.value = value;
    }

    public Object unwrap() {
        this.lastAccessTime = System.currentTimeMillis();
        this.accessCounter++;
        return value;
    }
}
