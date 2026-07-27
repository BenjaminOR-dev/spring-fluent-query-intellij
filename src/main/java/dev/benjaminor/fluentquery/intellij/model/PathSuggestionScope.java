package dev.benjaminor.fluentquery.intellij.model;

import org.jetbrains.annotations.NotNull;

/**
 * Which property kinds to offer in path completion.
 */
public enum PathSuggestionScope {
    /** Basics, associations and embeddeds (e.g. {@code select} segments). */
    ANY,
    /** Associations / embeddeds only ({@code fetch}, {@code whereHas}, …). */
    ASSOCIATIONS_ONLY,
    /**
     * Scalar columns only ({@code where}, related leaf column, select shorthand after {@code :}).
     * Relations belong on {@code whereHas} / {@code whereRelated*} / {@code whereRelation}.
     */
    ATTRIBUTES_ONLY;

    public boolean accepts(@NotNull JpaPropertyKind kind) {
        return switch (this) {
            case ANY -> true;
            case ASSOCIATIONS_ONLY -> kind.canNest();
            case ATTRIBUTES_ONLY -> kind == JpaPropertyKind.BASIC;
        };
    }
}
