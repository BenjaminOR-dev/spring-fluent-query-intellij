package dev.benjaminor.fluentquery.intellij.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

/**
 * Maps FluentQuery / RelatedFilter / FetchRel method names to path-argument roles.
 */
public final class FluentQueryMethods {

    public static final String FQ_FLUENT_QUERY = "dev.benjaminor.fluentquery.FluentQuery";
    public static final String FQ_RELATED_FILTER = "dev.benjaminor.fluentquery.RelatedFilter";
    public static final String FQ_FETCH_REL = "dev.benjaminor.fluentquery.FetchRel";
    public static final String FQ_REPOSITORY = "dev.benjaminor.fluentquery.FluentQueryRepository";

    private static final Set<String> ATTRIBUTE_METHODS = Set.of(
            "where", "whereEqual", "whereEqualIgnoreCase", "whereNotEqual",
            "whereLike", "whereContains", "whereStartsWith", "whereEndsWith", "whereLikePattern",
            "whereIn", "whereNotIn", "whereNull", "whereNotNull",
            "whereGt", "whereGreaterThan", "whereGte", "whereGreaterThanOrEqualTo",
            "whereLt", "whereLessThan", "whereLte", "whereLessThanOrEqualTo",
            "whereBetween", "whereNotBetween",
            "whereDate", "whereYear", "whereMonth", "whereDay", "whereTime",
            "orWhere", "orWhereLike", "orWhereContains", "orWhereStartsWith", "orWhereEndsWith",
            "orWhereLikePattern", "orWhereIn", "orWhereNotIn",
            "optionalWhere", "optionalWhereEqual", "optionalWhereEqualIgnoreCase", "optionalWhereNotEqual",
            "optionalWhereLike", "optionalWhereContains", "optionalWhereStartsWith", "optionalWhereEndsWith",
            "optionalWhereLikePattern", "optionalWhereIn", "optionalWhereNotIn",
            "optionalWhereGt", "optionalWhereGreaterThan", "optionalWhereGte", "optionalWhereGreaterThanOrEqualTo",
            "optionalWhereLt", "optionalWhereLessThan", "optionalWhereLte", "optionalWhereLessThanOrEqualTo",
            "optionalWhereBetween", "optionalWhereNotBetween",
            "optionalWhereDate", "optionalWhereYear", "optionalWhereMonth", "optionalWhereDay", "optionalWhereTime",
            "optionalOrWhere", "optionalOrWhereLike", "optionalOrWhereIn", "optionalOrWhereNotEqual", "optionalOrWhereNotIn",
            "latest", "latestOrNull", "oldest", "oldestOrNull"
    );

    private static final Set<String> ATTRIBUTE_BOOLEAN_FIRST = Set.of("whereIf", "whereEqualIf");

    private static final Set<String> ATTRIBUTE_PAIR = Set.of("whereColumn", "orWhereColumn");

    private static final Set<String> ORDER_METHODS = Set.of("orderByAsc", "orderByDesc");

    private static final Set<String> ASSOCIATION_METHODS = Set.of(
            "whereHas", "whereDoesntHave", "orWhereHas", "orWhereDoesntHave",
            "fetch", "with", "fetchCollection", "withCollection"
    );

    private static final Set<String> RELATION_THEN_ATTRIBUTE = Set.of(
            "whereRelatedEqual", "whereRelatedLike", "whereRelation",
            "optionalWhereRelatedEqual", "optionalWhereRelatedLike", "optionalWhereRelation"
    );

    private static final Set<String> SELECT_METHODS = Set.of("select");

    private FluentQueryMethods() {
    }

    public static boolean isFluentQueryFamily(@Nullable String qualifiedClassName) {
        return FQ_FLUENT_QUERY.equals(qualifiedClassName)
                || FQ_RELATED_FILTER.equals(qualifiedClassName)
                || FQ_FETCH_REL.equals(qualifiedClassName);
    }

    public static boolean isKnownMethodName(@NotNull String name) {
        return ATTRIBUTE_METHODS.contains(name)
                || ATTRIBUTE_BOOLEAN_FIRST.contains(name)
                || ATTRIBUTE_PAIR.contains(name)
                || ORDER_METHODS.contains(name)
                || ASSOCIATION_METHODS.contains(name)
                || RELATION_THEN_ATTRIBUTE.contains(name)
                || SELECT_METHODS.contains(name);
    }

    /**
     * @param argIndex 0-based index of the string literal in the argument list
     * @return role for that argument, or {@code null} if it is not a path argument
     */
    public static @Nullable FluentQueryPathRole roleFor(
            @NotNull String methodName,
            @Nullable String containingClassFqcn,
            int argIndex,
            int argCount) {
        // FetchRel.of(path) — only when resolved to FetchRel (avoid Optional.of, etc.)
        if ("of".equals(methodName) && FQ_FETCH_REL.equals(containingClassFqcn) && argIndex == 0) {
            return FluentQueryPathRole.ASSOCIATION;
        }
        if (containingClassFqcn == null || isFluentQueryFamily(containingClassFqcn)) {
            return roleByName(methodName, argIndex, argCount);
        }
        return null;
    }

    private static @Nullable FluentQueryPathRole roleByName(
            @NotNull String methodName, int argIndex, int argCount) {
        if (SELECT_METHODS.contains(methodName)) {
            return FluentQueryPathRole.SELECT;
        }
        if (ASSOCIATION_METHODS.contains(methodName)) {
            return FluentQueryPathRole.ASSOCIATION;
        }
        if (RELATION_THEN_ATTRIBUTE.contains(methodName)) {
            if (argIndex == 0) {
                return FluentQueryPathRole.ASSOCIATION;
            }
            if (argIndex == 1) {
                return FluentQueryPathRole.ATTRIBUTE;
            }
            return null;
        }
        if (ATTRIBUTE_PAIR.contains(methodName)) {
            // whereColumn(left, right) or whereColumn(left, op, right)
            if (argIndex == 0) {
                return FluentQueryPathRole.ATTRIBUTE;
            }
            if (argCount == 2 && argIndex == 1) {
                return FluentQueryPathRole.ATTRIBUTE;
            }
            if (argCount == 3 && argIndex == 2) {
                return FluentQueryPathRole.ATTRIBUTE;
            }
            return null;
        }
        if (ATTRIBUTE_BOOLEAN_FIRST.contains(methodName)) {
            return argIndex == 1 ? FluentQueryPathRole.ATTRIBUTE : null;
        }
        if (ORDER_METHODS.contains(methodName)) {
            return FluentQueryPathRole.ATTRIBUTE;
        }
        if (ATTRIBUTE_METHODS.contains(methodName)) {
            return argIndex == 0 ? FluentQueryPathRole.ATTRIBUTE : null;
        }
        return null;
    }

    public static boolean isRelationThenAttribute(@NotNull String methodName) {
        return RELATION_THEN_ATTRIBUTE.contains(methodName);
    }

    public static boolean looksLikeFluentChainMethod(@NotNull String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return isKnownMethodName(name)
                || n.startsWith("where")
                || n.startsWith("orwhere")
                || n.startsWith("optional")
                || n.startsWith("fetch")
                || n.startsWith("with")
                || "query".equals(name)
                || "select".equals(name);
    }
}
