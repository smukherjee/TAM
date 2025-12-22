package com.tam.platform.context;

import java.util.concurrent.Callable;

/**
 * Service to manage the current TenantContext.
 * Currently uses ThreadLocal, designed to be migrated to ScopedValue in the future.
 */
public class TenantContextService {
    private static final ThreadLocal<TenantContext> CURRENT_CONTEXT = ThreadLocal.withInitial(TenantContext::empty);

    /**
     * Sets the current context.
     * @param context The context to set.
     */
    public static void set(TenantContext context) {
        CURRENT_CONTEXT.set(context);
    }

    /**
     * Gets the current context.
     * @return The current TenantContext, or an empty context if none is set.
     */
    public static TenantContext get() {
        return CURRENT_CONTEXT.get();
    }

    /**
     * Clears the current context.
     */
    public static void clear() {
        CURRENT_CONTEXT.remove();
    }

    /**
     * Executes a Callable with the specified context.
     * Restores the previous context after execution.
     *
     * @param context The context to use.
     * @param action  The action to execute.
     * @param <T>     The return type.
     * @return The result of the action.
     * @throws Exception If the action throws an exception.
     */
    public static <T> T runWithContext(TenantContext context, Callable<T> action) throws Exception {
        TenantContext previous = get();
        try {
            set(context);
            return action.call();
        } finally {
            set(previous);
        }
    }

    /**
     * Executes a Runnable with the specified context.
     * Restores the previous context after execution.
     *
     * @param context The context to use.
     * @param action  The action to execute.
     */
    public static void runWithContext(TenantContext context, Runnable action) {
        TenantContext previous = get();
        try {
            set(context);
            action.run();
        } finally {
            set(previous);
        }
    }
}
