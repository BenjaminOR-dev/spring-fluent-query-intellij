package dev.benjaminor.fluentquery.intellij.model;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Expands Fluent Query {@code select} shorthand tokens
 * ({@code "status:id,name"} → {@code status.id}, {@code status.name}).
 *
 * <p>Mirrors {@code SelectPaths} in spring-fluent-query-core.
 */
public final class SelectPathExpander {

    private SelectPathExpander() {
    }

    public static @NotNull List<String> expand(@NotNull String token) {
        String trimmed = token.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        int colon = trimmed.indexOf(':');
        if (colon < 0) {
            return List.of(trimmed);
        }
        String prefix = trimmed.substring(0, colon).trim();
        String cols = trimmed.substring(colon + 1).trim();
        if (prefix.isEmpty() || cols.isEmpty()) {
            return List.of(trimmed);
        }
        List<String> out = new ArrayList<>();
        for (String col : cols.split(",")) {
            String c = col.trim();
            if (!c.isEmpty()) {
                out.add(prefix + "." + c);
            }
        }
        return out.isEmpty() ? List.of(trimmed) : List.copyOf(out);
    }
}
