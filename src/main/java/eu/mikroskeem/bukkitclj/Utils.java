/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.bukkitclj;

import com.pivovarit.function.ThrowingConsumer;
import com.pivovarit.function.ThrowingRunnable;
import com.pivovarit.function.ThrowingSupplier;

/**
 * @author Mark Vainomaa
 */
public final class Utils {
    private Utils() {}

    public static <T> T get(ThrowingSupplier<T, Exception> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> void run(ThrowingSupplier<T, Exception> supplier) {
        get(supplier);
    }

    public static void run(ThrowingRunnable<Exception> runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> void run(T arg, ThrowingConsumer<T, Exception> consumer) {
        try {
            consumer.accept(arg);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T apply(T arg, ThrowingConsumer<T, Exception> consumer) {
        run(arg, consumer);
        return arg;
    }
}
