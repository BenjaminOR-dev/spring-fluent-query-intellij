package dev.benjaminor.fluentquery.intellij.model;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Sibling string path args in the same FluentQuery call (varargs {@code select}/{@code fetch}/…).
 */
public final class SiblingPathArgs {

    private SiblingPathArgs() {
    }

    /** Methods where repeating the same path string is almost certainly a mistake. */
    public static boolean tracksDuplicates(@NotNull String methodName) {
        return "select".equals(methodName)
                || "fetch".equals(methodName)
                || "with".equals(methodName)
                || "fetchCollection".equals(methodName)
                || "withCollection".equals(methodName)
                || "orderByAsc".equals(methodName)
                || "orderByDesc".equals(methodName);
    }

    /**
     * Other string literals in the same call that share this site's role
     * (excludes {@code site.literal()} itself).
     */
    public static @NotNull Set<String> otherPaths(@NotNull FluentQueryCallSite site) {
        Set<String> out = new LinkedHashSet<>();
        if (!tracksDuplicates(site.methodName())) {
            return out;
        }
        PsiMethodCallExpression call = site.call();
        PsiExpression[] args = call.getArgumentList().getExpressions();
        for (PsiExpression arg : args) {
            if (!(arg instanceof PsiLiteralExpression lit) || lit == site.literal()) {
                continue;
            }
            if (!(lit.getValue() instanceof String raw)) {
                continue;
            }
            String path = PathStrings.stripCompletionDummy(raw).trim();
            if (path.isEmpty()) {
                continue;
            }
            FluentQueryPathRole role = FluentQueryMethods.roleFor(
                    site.methodName(),
                    FluentQueryMethods.FQ_FLUENT_QUERY,
                    indexOf(args, lit),
                    args.length);
            if (role == site.role()) {
                out.add(path);
            }
        }
        return out;
    }

    /**
     * Canonical path keys already taken by siblings (and, for select shorthand, columns
     * already listed after {@code ':'} in the current token being edited).
     */
    public static @NotNull Set<String> takenCanonicalPaths(
            @NotNull FluentQueryCallSite site, @NotNull String currentPrefix) {
        Set<String> taken = new HashSet<>();
        for (String sibling : otherPaths(site)) {
            taken.addAll(canonicalKeys(site.role(), sibling));
        }
        if (site.role() == FluentQueryPathRole.SELECT && currentPrefix.indexOf(':') >= 0) {
            int colon = currentPrefix.indexOf(':');
            String assoc = currentPrefix.substring(0, colon).trim();
            String after = currentPrefix.substring(colon + 1);
            int lastComma = after.lastIndexOf(',');
            // Columns already typed before the one under the caret
            String prior = lastComma >= 0 ? after.substring(0, lastComma) : "";
            for (String col : prior.split(",")) {
                String c = col.trim();
                if (!c.isEmpty() && !assoc.isEmpty()) {
                    taken.add(normalize(assoc + "." + c));
                    taken.add(normalize(c));
                }
            }
        }
        return taken;
    }

    public static boolean wouldDuplicate(
            @NotNull Set<String> takenCanonical,
            @NotNull FluentQueryPathRole role,
            @NotNull String pathPrefix,
            @NotNull String suggestedSegment) {
        String parent;
        if (role == FluentQueryPathRole.SELECT && pathPrefix.indexOf(':') >= 0) {
            int colon = pathPrefix.indexOf(':');
            String assoc = pathPrefix.substring(0, colon).trim();
            String proposed = assoc.isEmpty() ? suggestedSegment : assoc + "." + suggestedSegment;
            return takenCanonical.contains(normalize(proposed))
                    || takenCanonical.contains(normalize(suggestedSegment));
        }
        int lastDot = pathPrefix.lastIndexOf('.');
        parent = lastDot >= 0 ? pathPrefix.substring(0, lastDot) : "";
        String proposed = parent.isEmpty() ? suggestedSegment : parent + "." + suggestedSegment;
        return takenCanonical.contains(normalize(proposed));
    }

    /**
     * Issues for duplicate paths: only the current literal is reported when it repeats an
     * earlier sibling (or repeats a column inside select shorthand).
     */
    public static @NotNull List<PathValidator.Issue> duplicateIssues(@NotNull FluentQueryCallSite site) {
        List<PathValidator.Issue> issues = new ArrayList<>();
        if (!tracksDuplicates(site.methodName())) {
            return issues;
        }
        String path = site.pathText().trim();
        if (path.isEmpty()) {
            return issues;
        }

        // Duplicate columns inside one select shorthand token
        if (site.role() == FluentQueryPathRole.SELECT && path.indexOf(':') >= 0) {
            int colon = path.indexOf(':');
            String after = path.substring(colon + 1);
            Set<String> seenCols = new HashSet<>();
            for (String col : after.split(",", -1)) {
                String c = col.trim();
                if (c.isEmpty()) {
                    continue;
                }
                String key = normalize(c);
                if (!seenCols.add(key)) {
                    issues.add(PathValidator.Issue.of(
                            path, 0, path.length(),
                            "inspection.unresolved.path.duplicate",
                            path, site.methodName()));
                    return issues;
                }
            }
        }

        PsiMethodCallExpression call = site.call();
        PsiExpression[] args = call.getArgumentList().getExpressions();
        Set<String> seen = new HashSet<>();
        for (PsiExpression arg : args) {
            if (!(arg instanceof PsiLiteralExpression lit)) {
                continue;
            }
            if (!(lit.getValue() instanceof String raw)) {
                continue;
            }
            String siblingPath = PathStrings.stripCompletionDummy(raw).trim();
            if (siblingPath.isEmpty()) {
                continue;
            }
            int idx = indexOf(args, lit);
            FluentQueryPathRole role = FluentQueryMethods.roleFor(
                    site.methodName(), FluentQueryMethods.FQ_FLUENT_QUERY, idx, args.length);
            if (role != site.role()) {
                continue;
            }
            for (String key : canonicalKeys(role, siblingPath)) {
                if (!seen.add(key)) {
                    if (lit == site.literal()) {
                        issues.add(PathValidator.Issue.of(
                                path, 0, path.length(),
                                "inspection.unresolved.path.duplicate",
                                path, site.methodName()));
                    }
                    // keep scanning so later duplicates also get marked when validated
                }
            }
        }
        return issues;
    }

    private static @NotNull Set<String> canonicalKeys(
            @NotNull FluentQueryPathRole role, @NotNull String path) {
        Set<String> keys = new LinkedHashSet<>();
        if (role == FluentQueryPathRole.SELECT) {
            for (String expanded : SelectPathExpander.expand(path)) {
                keys.add(normalize(expanded));
            }
        } else {
            keys.add(normalize(path));
        }
        return keys;
    }

    private static @NotNull String normalize(@NotNull String path) {
        return path.trim().toLowerCase(Locale.ROOT);
    }

    private static int indexOf(PsiExpression @NotNull [] expressions, @NotNull PsiLiteralExpression literal) {
        for (int i = 0; i < expressions.length; i++) {
            if (expressions[i] == literal) {
                return i;
            }
        }
        return -1;
    }
}
