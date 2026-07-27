package dev.benjaminor.fluentquery.intellij.model;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One named property on an entity / embeddable graph node.
 */
public final class JpaProperty {

    private final @NotNull String name;
    private final @NotNull JpaPropertyKind kind;
    private final @Nullable PsiClass targetType;
    private final @NotNull PsiElement navigationElement;

    public JpaProperty(
            @NotNull String name,
            @NotNull JpaPropertyKind kind,
            @Nullable PsiClass targetType,
            @NotNull PsiElement navigationElement) {
        this.name = name;
        this.kind = kind;
        this.targetType = targetType;
        this.navigationElement = navigationElement;
    }

    public @NotNull String name() {
        return name;
    }

    public @NotNull JpaPropertyKind kind() {
        return kind;
    }

    public @Nullable PsiClass targetType() {
        return targetType;
    }

    public @NotNull PsiElement navigationElement() {
        return navigationElement;
    }
}
