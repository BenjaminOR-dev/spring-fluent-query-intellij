package dev.benjaminor.fluentquery.intellij.model;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Outcome of resolving a dotted (or single-segment) path against a {@link JpaEntityGraph}.
 */
public final class PathResolveResult {

    private final boolean resolved;
    private final @NotNull List<ResolvedSegment> segments;
    private final @Nullable String unresolvedSegment;
    private final int unresolvedIndex;
    private final @Nullable PsiClass tipEntity;

    private PathResolveResult(
            boolean resolved,
            @NotNull List<ResolvedSegment> segments,
            @Nullable String unresolvedSegment,
            int unresolvedIndex,
            @Nullable PsiClass tipEntity) {
        this.resolved = resolved;
        this.segments = List.copyOf(segments);
        this.unresolvedSegment = unresolvedSegment;
        this.unresolvedIndex = unresolvedIndex;
        this.tipEntity = tipEntity;
    }

    public static @NotNull PathResolveResult ok(
            @NotNull List<ResolvedSegment> segments,
            @Nullable PsiClass tipEntity) {
        return new PathResolveResult(true, segments, null, -1, tipEntity);
    }

    public static @NotNull PathResolveResult fail(
            @NotNull List<ResolvedSegment> resolvedPrefix,
            @NotNull String unresolvedSegment,
            int unresolvedIndex,
            @Nullable PsiClass tipBeforeFail) {
        return new PathResolveResult(false, resolvedPrefix, unresolvedSegment, unresolvedIndex, tipBeforeFail);
    }

    public boolean isResolved() {
        return resolved;
    }

    public @NotNull List<ResolvedSegment> segments() {
        return segments;
    }

    public @Nullable String unresolvedSegment() {
        return unresolvedSegment;
    }

    public int unresolvedIndex() {
        return unresolvedIndex;
    }

    /** Entity / embeddable at the tip after successful resolve (or before failure). */
    public @Nullable PsiClass tipEntity() {
        return tipEntity;
    }

    public record ResolvedSegment(
            @NotNull String name,
            int startOffsetInPath,
            int endOffsetInPath,
            @NotNull JpaProperty property) {

        public @NotNull PsiElement navigationElement() {
            return property.navigationElement();
        }
    }
}
