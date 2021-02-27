package com.root;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
class ValueWrapper {
    private Object value;
    private long lastAccessTime;

    public ValueWrapper(Object value) {
        this.value = value;
    }

    public Object getValue() {
        this.lastAccessTime = System.currentTimeMillis();
        return value;
    }
}
