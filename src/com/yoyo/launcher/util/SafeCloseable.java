package com.yoyo.launcher.util;

/**
 * An extension of AutoCloseable that does not throw any exceptions.
 */
public interface SafeCloseable extends AutoCloseable {
    @Override
    void close();
}
