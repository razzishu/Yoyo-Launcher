package com.yoyo.launcher.util;

/**
 * Utility interface for representing flag operations
 */
public interface FlagOp {

    FlagOp NO_OP = i -> i;

    int apply(int flags);

    default FlagOp addFlag(int flag) {
        return i -> apply(i) | flag;
    }

    default FlagOp removeFlag(int flag) {
        return i -> apply(i) & ~flag;
    }

    default FlagOp setFlag(int flag, boolean enable) {
        return enable ? addFlag(flag) : removeFlag(flag);
    }
}
