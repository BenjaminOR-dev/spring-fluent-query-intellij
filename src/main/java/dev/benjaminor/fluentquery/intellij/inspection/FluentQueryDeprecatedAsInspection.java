package dev.benjaminor.fluentquery.intellij.inspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethodCallExpression;
import dev.benjaminor.fluentquery.intellij.FluentQueryBundle;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryChain;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryMethods;
import dev.benjaminor.fluentquery.intellij.quickfix.MigrateDeprecatedAsQuickFix;
import org.jetbrains.annotations.NotNull;

/**
 * Warns on deprecated {@code *As} projection terminals; offers migration to Class overloads.
 */
public final class FluentQueryDeprecatedAsInspection extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public @NotNull String getShortName() {
        return "FluentQueryDeprecatedAs";
    }

    @Override
    public @NotNull String getDisplayName() {
        return FluentQueryBundle.message("inspection.deprecated.as.display.name");
    }

    @Override
    public @NotNull String getGroupDisplayName() {
        return FluentQueryBundle.message("inspection.group.name");
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
                String name = FluentQueryChain.resolveMethodName(expression);
                if (name == null || !FluentQueryMethods.isDeprecatedAsTerminal(name)) {
                    return;
                }
                if (!FluentQueryChain.isFluentQueryFamilyCall(expression)
                        && !FluentQueryMethods.isKnownMethodName(name)) {
                    return;
                }
                String preferred = FluentQueryMethods.preferredNameForDeprecatedAs(name);
                if (preferred == null) {
                    return;
                }
                holder.registerProblem(
                        expression.getMethodExpression().getReferenceNameElement() != null
                                ? expression.getMethodExpression().getReferenceNameElement()
                                : expression.getMethodExpression(),
                        FluentQueryBundle.message("inspection.deprecated.as.message", name, preferred),
                        ProblemHighlightType.LIKE_DEPRECATED,
                        new MigrateDeprecatedAsQuickFix());
            }
        };
    }
}
