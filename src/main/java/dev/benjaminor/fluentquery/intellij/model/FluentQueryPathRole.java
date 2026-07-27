package dev.benjaminor.fluentquery.intellij.model;

/**
 * How a string argument is interpreted against the entity graph.
 */
public enum FluentQueryPathRole {
    /** Single attribute on the current entity (no dots). */
    ATTRIBUTE,
    /** Association / embedded path (dots allowed). */
    ASSOCIATION,
    /** {@code select(...)} token (dots + optional {@code assoc:col1,col2} shorthand). */
    SELECT
}
