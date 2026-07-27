package dev.benjaminor.fluentquery.intellij.model;

/**
 * Kind of a persistent property on a JPA entity / embeddable.
 */
public enum JpaPropertyKind {
    /** Scalar / basic column (or unknown non-association). */
    BASIC,
    /** {@code @ManyToOne} / {@code @OneToOne}. */
    ASSOCIATION_TO_ONE,
    /** {@code @OneToMany} / {@code @ManyToMany}. */
    ASSOCIATION_TO_MANY,
    /** {@code @Embedded} / embeddable type. */
    EMBEDDED;

    public boolean isAssociation() {
        return this == ASSOCIATION_TO_ONE || this == ASSOCIATION_TO_MANY;
    }

    public boolean canNest() {
        return isAssociation() || this == EMBEDDED;
    }
}
