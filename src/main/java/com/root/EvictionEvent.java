package com.root;

@FunctionalInterface
public interface EvictionEvent {
    void doSmth(Object key);
}
