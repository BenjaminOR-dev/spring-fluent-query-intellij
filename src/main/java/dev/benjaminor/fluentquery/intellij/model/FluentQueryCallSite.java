package dev.benjaminor.fluentquery.intellij.model;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Analyzed FluentQuery / RelatedFilter / FetchRel string-path call site.
 */
public final class FluentQueryCallSite {

    private final @NotNull PsiLiteralExpression literal;
    private final @NotNull PsiMethodCallExpression call;
    private final @NotNull FluentQueryPathRole role;
    private final @NotNull PsiClass entityType;
    private final @NotNull String pathText;
    private final @NotNull String methodName;

    public FluentQueryCallSite(
            @NotNull PsiLiteralExpression literal,
            @NotNull PsiMethodCallExpression call,
            @NotNull FluentQueryPathRole role,
            @NotNull PsiClass entityType,
            @NotNull String pathText,
            @NotNull String methodName) {
        this.literal = literal;
        this.call = call;
        this.role = role;
        this.entityType = entityType;
        this.pathText = pathText;
        this.methodName = methodName;
    }

    public @NotNull PsiLiteralExpression literal() {
        return literal;
    }

    public @NotNull PsiMethodCallExpression call() {
        return call;
    }

    public @NotNull FluentQueryPathRole role() {
        return role;
    }

    public @NotNull PsiClass entityType() {
        return entityType;
    }

    public @NotNull String pathText() {
        return pathText;
    }

    public @NotNull String methodName() {
        return methodName;
    }

    public boolean associationsOnly() {
        return role == FluentQueryPathRole.ASSOCIATION;
    }
}
