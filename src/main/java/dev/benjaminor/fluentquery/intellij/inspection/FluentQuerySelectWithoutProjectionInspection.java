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
import org.jetbrains.annotations.NotNull;

/**
 * Soft hint: {@code select(...)} with an entity terminal (no projection {@code Class}) —
 * JPA cannot return a true partial entity.
 */
public final class FluentQuerySelectWithoutProjectionInspection
        extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public @NotNull String getShortName() {
        return "FluentQuerySelectWithoutProjection";
    }

    @Override
    public @NotNull String getDisplayName() {
        return FluentQueryBundle.message("inspection.select.without.projection.display.name");
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
                String terminal = FluentQueryChain.resolveMethodName(expression);
                if (terminal == null) {
                    return;
                }
                int argCount = expression.getArgumentList().getExpressions().length;
                if (!FluentQueryMethods.isEntityResultTerminalCall(terminal, argCount)) {
                    return;
                }
                if (!FluentQueryChain.isFluentQueryFamilyCall(expression)
                        && !FluentQueryMethods.isKnownMethodName(terminal)) {
                    return;
                }
                String select = FluentQueryChain.findInQualifierChain(expression, "select"::equals);
                if (select == null) {
                    return;
                }
                // skip if fetch is present — entity + fetch is intentional
                if (FluentQueryChain.findInQualifierChain(
                        expression, FluentQueryMethods::isFetchChainMethod) != null) {
                    return;
                }
                holder.registerProblem(
                        expression.getMethodExpression().getReferenceNameElement() != null
                                ? expression.getMethodExpression().getReferenceNameElement()
                                : expression.getMethodExpression(),
                        FluentQueryBundle.message(
                                "inspection.select.without.projection.message", terminal),
                        ProblemHighlightType.WEAK_WARNING);
            }
        };
    }
}
