package dev.benjaminor.fluentquery.intellij.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

/**
 * Maps FluentQuery / RelatedFilter / FetchRel / PropertyFilters method names to path roles.
 */
public final class FluentQueryMethods {

    public static final String FQ_FLUENT_QUERY = "dev.benjaminor.fluentquery.FluentQuery";
    public static final String FQ_RELATED_FILTER = "dev.benjaminor.fluentquery.RelatedFilter";
    public static final String FQ_FETCH_REL = "dev.benjaminor.fluentquery.FetchRel";
    public static final String FQ_REPOSITORY = "dev.benjaminor.fluentquery.FluentQueryRepository";
    public static final String FQ_PROPERTY_FILTERS = "dev.benjaminor.fluentquery.PropertyFilters";

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
            // PropertyFilters (direct on repository)
            "hasPropertyLike", "hasPropertyLikeEscaped", "hasPropertyLikePattern",
            "hasPropertyEqual", "hasPropertyNotEqual",
            "hasPropertyIn", "hasPropertyNotIn",
            "hasPropertyIsNull", "hasPropertyIsNotNull",
            "hasPropertyGreaterThan", "hasPropertyGreaterThanOrEqualTo",
            "hasPropertyLessThan", "hasPropertyLessThanOrEqualTo",
            "hasPropertyBetween"
    );

    private static final Set<String> ATTRIBUTE_BOOLEAN_FIRST = Set.of("whereIf", "whereEqualIf");

    private static final Set<String> ATTRIBUTE_PAIR = Set.of("whereColumn", "orWhereColumn");

    /** Spring Data property paths (dots OK, leaf must be scalar). */
    private static final Set<String> PROPERTY_PATH_METHODS = Set.of(
            "orderByAsc", "orderByDesc",
            "latest", "latestOrNull", "latestOrFail", "oldest", "oldestOrNull", "oldestOrFail",
            // deprecated *As aliases
            "latestAs", "latestAsOrFail", "latestAsOrNull",
            "oldestAs", "oldestAsOrFail", "oldestAsOrNull"
    );

    /**
     * Deprecated {@code *As} terminals — always projections.
     */
    private static final Set<String> PROJECTION_AS_TERMINALS = Set.of(
            "firstAs", "firstAsOrFail", "firstAsOrNull",
            "oneAs", "oneAsOrFail", "oneAsOrNull",
            "latestAs", "latestAsOrFail", "latestAsOrNull",
            "oldestAs", "oldestAsOrFail", "oldestAsOrNull",
            "getAs", "pageAs", "paginateAs", "sliceAs"
    );

    /**
     * Terminals that become projections when a {@code Class} argument is present
     * ({@code first(Class)}, {@code page(pageable, Class)}, …).
     */
    private static final Set<String> PROJECTION_CLASS_OVERLOADS = Set.of(
            "first", "firstOrFail", "firstOrNull",
            "one", "oneOrFail", "oneOrNull",
            "latest", "latestOrFail", "latestOrNull",
            "oldest", "oldestOrFail", "oldestOrNull",
            "get", "page", "paginate", "slice"
    );

    private static final Set<String> FETCH_CHAIN_METHODS = Set.of(
            "fetch", "with", "fetchCollection", "withCollection"
    );

    private static final Set<String> FETCH_COLLECTION_METHODS = Set.of(
            "fetchCollection", "withCollection"
    );

    /** Runtime-incompatible with fetchCollection (cartesian / LIMIT on join product). */
    private static final Set<String> COLLECTION_FETCH_INCOMPATIBLE = Set.of(
            "page", "slice", "paginate", "chunk", "limit"
    );

    /** Entity terminals that ignore select column trimming (soft hint when select is present). */
    private static final Set<String> ENTITY_RESULT_TERMINALS = Set.of(
            "first", "firstOrFail", "firstOrNull",
            "one", "oneOrFail", "oneOrNull",
            "latest", "latestOrNull",
            "oldest", "oldestOrNull",
            "get", "page", "paginate", "slice", "stream"
    );

    private static final Set<String> ASSOCIATION_METHODS = Set.of(
            "whereHas", "whereDoesntHave", "orWhereHas", "orWhereDoesntHave",
            "fetch", "with", "fetchCollection", "withCollection"
    );

    /** PropertyFilters: single-segment association only (no dots). */
    private static final Set<String> ASSOCIATION_SINGLE_METHODS = Set.of(
            "hasRelation", "hasNoRelation"
    );

    private static final Set<String> RELATION_THEN_ATTRIBUTE = Set.of(
            "whereRelatedEqual", "whereRelatedLike", "whereRelation",
            "optionalWhereRelatedEqual", "optionalWhereRelatedLike", "optionalWhereRelation",
            "hasRelatedPropertyEqual", "hasRelatedPropertyLike"
    );

    private static final Set<String> SELECT_METHODS = Set.of("select");

    private static final Set<String> MAP_FACTORY_METHODS = Set.of("of", "ofEntries", "singletonMap");

    private FluentQueryMethods() {
    }

    public static boolean isFluentQueryFamily(@Nullable String qualifiedClassName) {
        return FQ_FLUENT_QUERY.equals(qualifiedClassName)
                || FQ_RELATED_FILTER.equals(qualifiedClassName)
                || FQ_FETCH_REL.equals(qualifiedClassName)
                || FQ_PROPERTY_FILTERS.equals(qualifiedClassName);
    }

    public static boolean isKnownMethodName(@NotNull String name) {
        return ATTRIBUTE_METHODS.contains(name)
                || ATTRIBUTE_BOOLEAN_FIRST.contains(name)
                || ATTRIBUTE_PAIR.contains(name)
                || PROPERTY_PATH_METHODS.contains(name)
                || ASSOCIATION_METHODS.contains(name)
                || ASSOCIATION_SINGLE_METHODS.contains(name)
                || RELATION_THEN_ATTRIBUTE.contains(name)
                || SELECT_METHODS.contains(name)
                || PROJECTION_AS_TERMINALS.contains(name)
                || PROJECTION_CLASS_OVERLOADS.contains(name)
                || COLLECTION_FETCH_INCOMPATIBLE.contains(name)
                || "stream".equals(name)
                || "exists".equals(name)
                || "count".equals(name)
                || "delete".equals(name);
    }

    /** Deprecated {@code *As} name, or any Class-overload terminal name (entity or projection). */
    public static boolean isProjectionTerminalName(@NotNull String name) {
        return PROJECTION_AS_TERMINALS.contains(name) || PROJECTION_CLASS_OVERLOADS.contains(name);
    }

    public static boolean isDeprecatedAsTerminal(@NotNull String name) {
        return PROJECTION_AS_TERMINALS.contains(name);
    }

    public static boolean isFetchCollectionMethod(@NotNull String name) {
        return FETCH_COLLECTION_METHODS.contains(name);
    }

    public static boolean isCollectionFetchIncompatible(@NotNull String name) {
        return COLLECTION_FETCH_INCOMPATIBLE.contains(name);
    }

    /**
     * Entity-result terminal (no projection Class): soft-warn when combined with {@code select}.
     */
    public static boolean isEntityResultTerminalCall(@NotNull String name, int argCount) {
        if (!ENTITY_RESULT_TERMINALS.contains(name)) {
            return false;
        }
        return !isProjectionTerminalCall(name, argCount);
    }

    /**
     * Preferred replacement name for a deprecated {@code *As} terminal, or {@code null}.
     */
    public static @Nullable String preferredNameForDeprecatedAs(@NotNull String asName) {
        if (!PROJECTION_AS_TERMINALS.contains(asName)) {
            return null;
        }
        if (asName.endsWith("AsOrFail")) {
            return asName.substring(0, asName.length() - "AsOrFail".length()) + "OrFail";
        }
        if (asName.endsWith("AsOrNull")) {
            return asName.substring(0, asName.length() - "AsOrNull".length()) + "OrNull";
        }
        if (asName.endsWith("As")) {
            return asName.substring(0, asName.length() - 2);
        }
        return null;
    }

    /** {@code true} when migrating {@code *As} requires swapping Class to the last argument. */
    public static boolean deprecatedAsNeedsArgReorder(@NotNull String asName) {
        return "pageAs".equals(asName) || "paginateAs".equals(asName) || "sliceAs".equals(asName);
    }

    /**
     * Whether this call is a projection terminal given argument count
     * (Class overloads need the extra {@code Class} arg; {@code *As} always count).
     */
    public static boolean isProjectionTerminalCall(@NotNull String name, int argCount) {
        if (PROJECTION_AS_TERMINALS.contains(name)) {
            return true;
        }
        if (!PROJECTION_CLASS_OVERLOADS.contains(name)) {
            return false;
        }
        return switch (name) {
            case "page", "slice" -> argCount >= 2;
            case "paginate" -> argCount >= 3;
            case "latest", "latestOrFail", "latestOrNull",
                 "oldest", "oldestOrFail", "oldestOrNull" -> argCount >= 2;
            default -> argCount >= 1; // first/one/get (+ OrFail/OrNull)
        };
    }

    /** @deprecated prefer {@link #isProjectionTerminalCall(String, int)} */
    @Deprecated
    public static boolean isProjectionTerminal(@NotNull String name) {
        return isProjectionTerminalName(name);
    }

    public static boolean isFetchChainMethod(@NotNull String name) {
        return FETCH_CHAIN_METHODS.contains(name);
    }

    public static boolean isAssociationMethodName(@NotNull String name) {
        return ASSOCIATION_METHODS.contains(name) || ASSOCIATION_SINGLE_METHODS.contains(name);
    }

    public static boolean isSingleSegmentAssociation(@NotNull String methodName) {
        return ASSOCIATION_SINGLE_METHODS.contains(methodName);
    }

    public static boolean isMapFactoryMethod(@NotNull String methodName) {
        return MAP_FACTORY_METHODS.contains(methodName);
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
        if (ASSOCIATION_METHODS.contains(methodName) || ASSOCIATION_SINGLE_METHODS.contains(methodName)) {
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
        if (PROPERTY_PATH_METHODS.contains(methodName)) {
            // latest/oldest(+As) with Class: only the sort column (arg 0) is a path
            if (methodName.startsWith("latest") || methodName.startsWith("oldest")) {
                return argIndex == 0 ? FluentQueryPathRole.PROPERTY_PATH : null;
            }
            return FluentQueryPathRole.PROPERTY_PATH;
        }
        if (PROJECTION_AS_TERMINALS.contains(methodName)) {
            return null;
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
                || n.startsWith("has")
                || "query".equals(name)
                || "select".equals(name);
    }
}
