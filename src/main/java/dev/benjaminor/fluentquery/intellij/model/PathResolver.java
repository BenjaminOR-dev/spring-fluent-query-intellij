package dev.benjaminor.fluentquery.intellij.model;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves dotted property paths against {@link JpaEntityGraph} nodes.
 */
public final class PathResolver {

    private PathResolver() {
    }

    /**
     * Resolves {@code path} starting at {@code rootEntity}.
     *
     * @param associationsOnly when {@code true}, every segment (including the last) must be nestable
     *                         (association / embedded); when {@code false}, intermediate segments must
     *                         be nestable and the last may be any property.
     */
    public static @NotNull PathResolveResult resolve(
            @NotNull PsiClass rootEntity,
            @NotNull String path,
            boolean associationsOnly) {
        String trimmed = path.trim();
        if (trimmed.isEmpty()) {
            return PathResolveResult.fail(List.of(), "", 0, rootEntity);
        }

        List<PathResolveResult.ResolvedSegment> segments = new ArrayList<>();
        PsiClass current = rootEntity;
        int offset = 0;
        String[] parts = trimmed.split("\\.", -1);
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            int start = offset;
            int end = offset + part.length();
            // advance offset past this segment and the following dot (if any)
            offset = end + (i < parts.length - 1 ? 1 : 0);

            if (part.isEmpty()) {
                return PathResolveResult.fail(segments, part, i, current);
            }
            if (current == null) {
                return PathResolveResult.fail(segments, part, i, null);
            }

            JpaEntityGraph graph = JpaEntityGraphResolver.resolve(current);
            JpaProperty property = graph.find(part);
            if (property == null) {
                return PathResolveResult.fail(segments, part, i, current);
            }

            boolean last = i == parts.length - 1;
            if (!last && !property.kind().canNest()) {
                return PathResolveResult.fail(segments, part, i, current);
            }
            if (associationsOnly && !property.kind().canNest()) {
                return PathResolveResult.fail(segments, part, i, current);
            }

            segments.add(new PathResolveResult.ResolvedSegment(part, start, end, property));
            if (!last || associationsOnly) {
                current = property.targetType();
            } else if (property.kind().canNest()) {
                current = property.targetType();
            }
        }

        PsiClass tip = segments.isEmpty()
                ? rootEntity
                : tipAfter(segments.get(segments.size() - 1).property(), associationsOnly);
        return PathResolveResult.ok(segments, tip != null ? tip : current);
    }

    /**
     * Completes suggestions for the segment under construction at {@code pathPrefix}.
     * Example: {@code "prof"} → properties of root; {@code "profile.a"} → properties of profile target.
     *
     * @param associationsOnly {@code true} → only nestable props; {@code false} → any property
     */
    public static @NotNull List<JpaProperty> complete(
            @NotNull PsiClass rootEntity,
            @NotNull String pathPrefix,
            boolean associationsOnly) {
        return complete(
                rootEntity,
                pathPrefix,
                associationsOnly ? PathSuggestionScope.ASSOCIATIONS_ONLY : PathSuggestionScope.ANY);
    }

    /**
     * Completes suggestions filtered by {@link PathSuggestionScope}.
     */
    public static @NotNull List<JpaProperty> complete(
            @NotNull PsiClass rootEntity,
            @NotNull String pathPrefix,
            @NotNull PathSuggestionScope suggestionScope) {
        String trimmed = pathPrefix.trim();
        int lastDot = trimmed.lastIndexOf('.');
        String parentPath = lastDot >= 0 ? trimmed.substring(0, lastDot) : "";
        String fragment = lastDot >= 0 ? trimmed.substring(lastDot + 1) : trimmed;

        PsiClass scope = rootEntity;
        if (!parentPath.isEmpty()) {
            PathResolveResult parent = resolve(rootEntity, parentPath, true);
            if (!parent.isResolved() || parent.tipEntity() == null) {
                return List.of();
            }
            scope = parent.tipEntity();
        }

        JpaEntityGraph graph = JpaEntityGraphResolver.resolve(scope);
        String lower = fragment.toLowerCase();
        List<JpaProperty> out = new ArrayList<>();
        for (JpaProperty p : graph.properties()) {
            if (!suggestionScope.accepts(p.kind())) {
                continue;
            }
            if (lower.isEmpty() || p.name().toLowerCase().startsWith(lower)) {
                out.add(p);
            }
        }
        return out;
    }

    private static @Nullable PsiClass tipAfter(@NotNull JpaProperty last, boolean associationsOnly) {
        if (associationsOnly || last.kind().canNest()) {
            return last.targetType();
        }
        return null;
    }
}
