package dev.benjaminor.fluentquery.intellij.model;

/**
 * How a string argument is interpreted against the entity graph.
 */
public enum FluentQueryPathRole {
    /**
     * Single scalar attribute on the current entity — no dots
     * ({@code where}, RelatedFilter columns, PropertyFilters {@code hasProperty*}).
     */
    ATTRIBUTE,
    /**
     * Association / embedded path — dots allowed, {@code ':'} forbidden
     * ({@code fetch}, {@code whereHas}, {@code FetchRel}, …).
     */
    ASSOCIATION,
    /**
     * Nested property path ending in a scalar (Spring Data style) —
     * {@code orderBy*}, {@code latest}/{@code oldest}. No {@code ':'}.
     */
    PROPERTY_PATH,
    /**
     * {@code select(...)} token: property path and/or {@code assoc:col1,col2} shorthand.
     * Expanded paths must end in a scalar column.
     */
    SELECT
}
