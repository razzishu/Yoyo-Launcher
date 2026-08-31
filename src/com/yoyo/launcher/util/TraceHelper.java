package com.yoyo.launcher.util;

import android.os.Trace;
import java.util.function.Supplier;

public class TraceHelper {
    public static final TraceHelper INSTANCE = new TraceHelper();
    
    public void beginSection(String label) {
        Trace.beginSection(label);
    }
    
    public void endSection() {
        Trace.endSection();
    }
    
    public static <T> T allowIpcs(String label, Supplier<T> supplier) {
        return supplier.get();
    }
}
