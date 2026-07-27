package dev.benjaminor.fluentquery.intellij.inspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

/**
 * Highlights FluentQuery path segments that do not resolve to an entity field or association.
 *
 * <p>Scaffold only — visitor logic comes with the MVP path model.
 */
public final class FluentQueryUnresolvedPathInspection extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return PsiElementVisitor.EMPTY_VISITOR;
    }
}
