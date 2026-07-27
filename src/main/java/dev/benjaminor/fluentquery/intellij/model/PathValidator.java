package dev.benjaminor.fluentquery.intellij.model;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.PropertyKey;

import java.util.ArrayList;
import java.util.List;

import static dev.benjaminor.fluentquery.intellij.FluentQueryBundle.BUNDLE;

/**
 * Validates a call-site path string according to its {@link FluentQueryPathRole}.
 */
public final class PathValidator {

    private PathValidator() {
    }

    public static @NotNull List<Issue> validate(@NotNull FluentQueryCallSite site) {
        return validate(site.entityType(), site.pathText(), site.role(), site.methodName());
    }

    public static @NotNull List<Issue> validate(
            @NotNull PsiClass entity,
            @NotNull String pathText,
            @NotNull FluentQueryPathRole role) {
        return validate(entity, pathText, role, "");
    }

    public static @NotNull List<Issue> validate(
            @NotNull PsiClass entity,
            @NotNull String pathText,
            @NotNull FluentQueryPathRole role,
            @NotNull String methodName) {
        List<Issue> issues = new ArrayList<>();
        String method = methodName.isBlank() ? "this method" : methodName;
        switch (role) {
            case ATTRIBUTE -> validateAttribute(entity, pathText, method, issues);
            case ASSOCIATION -> validateAssociation(entity, pathText, method, issues);
            case PROPERTY_PATH -> validatePropertyPath(entity, pathText, method, issues);
            case SELECT -> validateSelect(entity, pathText, issues);
        }
        return issues;
    }

    private static void validateAttribute(
            @NotNull PsiClass entity,
            @NotNull String pathText,
            @NotNull String method,
            @NotNull List<Issue> issues) {
        if (pathText.indexOf('.') >= 0) {
            issues.add(Issue.of(
                    pathText, 0, pathText.length(),
                    "inspection.unresolved.path.dotted.attribute",
                    pathText, method));
            return;
        }
        if (pathText.isEmpty()) {
            return;
        }
        PathResolveResult r = PathResolver.resolve(entity, pathText, false);
        if (!r.isResolved()) {
            issues.add(issueFrom(r, pathText));
            return;
        }
        JpaProperty last = lastProperty(r);
        if (last != null && last.kind() != JpaPropertyKind.BASIC) {
            issues.add(Issue.of(
                    pathText, 0, pathText.length(),
                    "inspection.unresolved.path.association.attribute",
                    pathText, method));
        }
    }

    private static void validateAssociation(
            @NotNull PsiClass entity,
            @NotNull String pathText,
            @NotNull String method,
            @NotNull List<Issue> issues) {
        if (pathText.indexOf(':') >= 0) {
            issues.add(Issue.of(
                    pathText, 0, pathText.length(),
                    "inspection.unresolved.path.colon.in.association",
                    pathText, method));
            return;
        }
        if (FluentQueryMethods.isSingleSegmentAssociation(method) && pathText.indexOf('.') >= 0) {
            issues.add(Issue.of(
                    pathText, 0, pathText.length(),
                    "inspection.unresolved.path.single.segment",
                    pathText, method));
            return;
        }
        if (hasEmptySegment(pathText)) {
            issues.add(Issue.of(
                    pathText, 0, pathText.length(),
                    "inspection.unresolved.path.empty.segment",
                    pathText));
            return;
        }
        if (pathText.isEmpty()) {
            return;
        }
        PathResolveResult r = PathResolver.resolve(entity, pathText, true);
        if (!r.isResolved()) {
            issues.add(issueFrom(r, pathText));
        }
    }

    private static void validatePropertyPath(
            @NotNull PsiClass entity,
            @NotNull String pathText,
            @NotNull String method,
            @NotNull List<Issue> issues) {
        if (pathText.indexOf(':') >= 0) {
            issues.add(Issue.of(
                    pathText, 0, pathText.length(),
                    "inspection.unresolved.path.colon.in.property",
                    pathText));
            return;
        }
        validateScalarLeafPath(entity, pathText, issues);
    }

    private static void validateSelect(
            @NotNull PsiClass entity,
            @NotNull String pathText,
            @NotNull List<Issue> issues) {
        if (pathText.indexOf(':') >= 0) {
            String shorthandError = selectShorthandError(pathText);
            if (shorthandError != null) {
                issues.add(Issue.of(
                        pathText, 0, pathText.length(),
                        "inspection.unresolved.path.select.shorthand",
                        pathText));
                return;
            }
        }
        for (String expanded : SelectPathExpander.expand(pathText)) {
            validateScalarLeafPath(entity, expanded, issues);
        }
    }

    private static void validateScalarLeafPath(
            @NotNull PsiClass entity,
            @NotNull String pathText,
            @NotNull List<Issue> issues) {
        if (pathText.isEmpty()) {
            return;
        }
        if (hasEmptySegment(pathText)) {
            issues.add(Issue.of(
                    pathText, 0, pathText.length(),
                    "inspection.unresolved.path.empty.segment",
                    pathText));
            return;
        }
        PathResolveResult r = PathResolver.resolve(entity, pathText, false);
        if (!r.isResolved()) {
            issues.add(issueFrom(r, pathText));
            return;
        }
        JpaProperty last = lastProperty(r);
        if (last != null && last.kind() != JpaPropertyKind.BASIC) {
            issues.add(Issue.of(
                    pathText, 0, pathText.length(),
                    "inspection.unresolved.path.non.basic.leaf",
                    pathText));
        }
    }

    private static @Nullable String selectShorthandError(@NotNull String token) {
        String trimmed = token.trim();
        int colon = trimmed.indexOf(':');
        if (colon < 0) {
            return null;
        }
        String prefix = trimmed.substring(0, colon).trim();
        String cols = trimmed.substring(colon + 1).trim();
        if (prefix.isEmpty() || cols.isEmpty()) {
            return "malformed";
        }
        for (String col : cols.split(",", -1)) {
            if (col.trim().isEmpty()) {
                return "blank column";
            }
        }
        return null;
    }

    private static boolean hasEmptySegment(@NotNull String pathText) {
        if (pathText.startsWith(".") || pathText.endsWith(".")) {
            return true;
        }
        return pathText.contains("..");
    }

    private static @Nullable JpaProperty lastProperty(@NotNull PathResolveResult r) {
        if (r.segments().isEmpty()) {
            return null;
        }
        return r.segments().get(r.segments().size() - 1).property();
    }

    private static @NotNull Issue issueFrom(@NotNull PathResolveResult r, @NotNull String fullPath) {
        String bad = r.unresolvedSegment() != null ? r.unresolvedSegment() : fullPath;
        if (bad != null && bad.isEmpty()) {
            return Issue.of(
                    fullPath, 0, fullPath.length(),
                    "inspection.unresolved.path.empty.segment",
                    fullPath);
        }
        if (r.segments().isEmpty()) {
            return Issue.of(
                    fullPath, 0, fullPath.length(),
                    "inspection.unresolved.path.message",
                    bad == null || bad.isEmpty() ? fullPath : bad);
        }
        int idx = r.unresolvedIndex();
        String[] parts = fullPath.split("\\.", -1);
        if (idx >= 0 && idx < parts.length && !fullPath.contains(":")) {
            int start = 0;
            for (int i = 0; i < idx; i++) {
                start += parts[i].length() + 1;
            }
            int end = start + parts[idx].length();
            return Issue.of(
                    fullPath, start, end,
                    "inspection.unresolved.path.message",
                    parts[idx]);
        }
        return Issue.of(
                fullPath, 0, fullPath.length(),
                "inspection.unresolved.path.message",
                bad == null ? fullPath : bad);
    }

    /**
     * @param displayPath  path shown in diagnostics
     * @param start        start offset within the string value (not including quotes)
     * @param end          end offset within the string value
     * @param messageKey   bundle key under {@code messages.FluentQueryBundle}
     * @param messageArgs  format args for the bundle message
     */
    public record Issue(
            @NotNull String displayPath,
            int start,
            int end,
            @NotNull @PropertyKey(resourceBundle = BUNDLE) String messageKey,
            Object @NotNull [] messageArgs) {

        public static @NotNull Issue of(
                @NotNull String displayPath,
                int start,
                int end,
                @NotNull @PropertyKey(resourceBundle = BUNDLE) String messageKey,
                Object @NotNull ... messageArgs) {
            return new Issue(displayPath, start, end, messageKey, messageArgs);
        }
    }
}
