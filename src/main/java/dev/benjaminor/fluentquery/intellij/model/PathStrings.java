package dev.benjaminor.fluentquery.intellij.model;

import com.intellij.codeInsight.completion.CompletionUtilCore;
import org.jetbrains.annotations.NotNull;

/**
 * Shared string helpers for path literals (including completion dummy identifiers).
 */
public final class PathStrings {

    private PathStrings() {
    }

    public static @NotNull String stripCompletionDummy(@NotNull String value) {
        return value
                .replace(CompletionUtilCore.DUMMY_IDENTIFIER, "")
                .replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "");
    }
}
