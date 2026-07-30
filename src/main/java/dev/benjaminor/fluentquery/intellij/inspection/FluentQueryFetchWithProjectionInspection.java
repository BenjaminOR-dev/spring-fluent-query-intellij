package dev.benjaminor.fluentquery.intellij.inspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethodCallExpression;
import dev.benjaminor.fluentquery.intellij.FluentQueryBundle;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryChain;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryMethods;
import dev.benjaminor.fluentquery.intellij.quickfix.RemoveFetchFromChainQuickFix;
import org.jetbrains.annotations.NotNull;

/**
 * Flags {@code fetch}/{@code with}* on a chain that ends in a projection terminal
 * ({@code first(Class)}, {@code get(Class)}, deprecated {@code *As}, …).
 */
public final class FluentQueryFetchWithProjectionInspection extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public @NotNull String getShortName() {
        return "FluentQueryFetchWithProjection";
    }

    @Override
    public @NotNull String getDisplayName() {
        return FluentQueryBundle.message("inspection.fetch.with.projection.display.name");
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
                if (!FluentQueryMethods.isProjectionTerminalCall(terminal, argCount)) {
                    return;
                }
                if (!FluentQueryChain.isFluentQueryFamilyCall(expression)) {
                    return;
                }
                String fetchMethod = FluentQueryChain.findInQualifierChain(
                        expression, FluentQueryMethods::isFetchChainMethod);
                if (fetchMethod == null) {
                    return;
                }
                holder.registerProblem(
                        expression.getMethodExpression().getReferenceNameElement() != null
                                ? expression.getMethodExpression().getReferenceNameElement()
                                : expression.getMethodExpression(),
                        FluentQueryBundle.message(
                                "inspection.fetch.with.projection.message", fetchMethod, terminal),
                        new RemoveFetchFromChainQuickFix());
            }
        };
    }
}
