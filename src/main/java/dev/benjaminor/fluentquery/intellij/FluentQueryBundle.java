package dev.benjaminor.fluentquery.intellij;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

public final class FluentQueryBundle extends DynamicBundle {

    public static final String BUNDLE = "messages.FluentQueryBundle";
    private static final FluentQueryBundle INSTANCE = new FluentQueryBundle();

    private FluentQueryBundle() {
        super(BUNDLE);
    }

    public static @NotNull @Nls String message(
            @NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
            Object @NotNull ... params) {
        return INSTANCE.getMessage(key, params);
    }

    public static @NotNull Supplier<@Nls String> messagePointer(
            @NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
            Object @NotNull ... params) {
        return INSTANCE.getLazyMessage(key, params);
    }
}
