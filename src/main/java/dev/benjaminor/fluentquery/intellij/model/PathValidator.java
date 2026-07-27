package dev.benjaminor.fluentquery.intellij.model;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
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
        return validate(site.entityType(), site.pathText(), site.role());
    }

    public static @NotNull List<Issue> validate(
            @NotNull PsiClass entity,
            @NotNull String pathText,
            @NotNull FluentQueryPathRole role) {
        List<Issue> issues = new ArrayList<>();
        switch (role) {
            case ATTRIBUTE -> {
                if (pathText.indexOf('.') >= 0) {
                    issues.add(new Issue(
                            pathText,
                            0,
                            pathText.length(),
                            pathText,
                            "inspection.unresolved.path.dotted.attribute"));
                    break;
                }
                PathResolveResult r = PathResolver.resolve(entity, pathText, false);
                if (!r.isResolved()) {
                    issues.add(issueFrom(r, pathText));
                }
            }
            case ASSOCIATION -> {
                PathResolveResult r = PathResolver.resolve(entity, pathText, true);
                if (!r.isResolved()) {
                    issues.add(issueFrom(r, pathText));
                }
            }
            case SELECT -> {
                for (String expanded : SelectPathExpander.expand(pathText)) {
                    PathResolveResult r = PathResolver.resolve(entity, expanded, false);
                    if (!r.isResolved()) {
                        issues.add(issueFrom(r, pathText.contains(":") ? pathText : expanded));
                    }
                }
            }
        }
        return issues;
    }

    private static @NotNull Issue issueFrom(@NotNull PathResolveResult r, @NotNull String fullPath) {
        String bad = r.unresolvedSegment() != null ? r.unresolvedSegment() : fullPath;
        if (r.segments().isEmpty()) {
            return new Issue(
                    fullPath,
                    0,
                    fullPath.length(),
                    bad.isEmpty() ? fullPath : bad,
                    "inspection.unresolved.path.message");
        }
        int idx = r.unresolvedIndex();
        String[] parts = fullPath.split("\\.", -1);
        if (idx >= 0 && idx < parts.length && !fullPath.contains(":")) {
            int start = 0;
            for (int i = 0; i < idx; i++) {
                start += parts[i].length() + 1;
            }
            int end = start + parts[idx].length();
            return new Issue(
                    fullPath,
                    start,
                    end,
                    parts[idx],
                    "inspection.unresolved.path.message");
        }
        return new Issue(
                fullPath,
                0,
                fullPath.length(),
                bad,
                "inspection.unresolved.path.message");
    }

    /**
     * @param displayPath path shown in diagnostics
     * @param start       start offset within the string value (not including quotes)
     * @param end         end offset within the string value
     * @param badSegment  segment that failed (message argument)
     * @param messageKey  bundle key under {@code messages.FluentQueryBundle}
     */
    public record Issue(
            @NotNull String displayPath,
            int start,
            int end,
            @NotNull String badSegment,
            @NotNull @PropertyKey(resourceBundle = BUNDLE) String messageKey) {
    }
}
