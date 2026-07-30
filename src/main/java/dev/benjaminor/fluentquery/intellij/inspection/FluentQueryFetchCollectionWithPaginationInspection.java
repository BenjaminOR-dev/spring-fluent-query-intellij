package dev.benjaminor.fluentquery.intellij.inspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethodCallExpression;
import dev.benjaminor.fluentquery.intellij.FluentQueryBundle;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryChain;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryMethods;
import dev.benjaminor.fluentquery.intellij.quickfix.RemoveFetchCollectionFromChainQuickFix;
import org.jetbrains.annotations.NotNull;

/**
 * Flags {@code fetchCollection}/{@code withCollection} combined with
 * {@code page}/{@code slice}/{@code paginate}/{@code chunk}/{@code limit}.
 */
public final class FluentQueryFetchCollectionWithPaginationInspection
        extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public @NotNull String getShortName() {
        return "FluentQueryFetchCollectionWithPagination";
    }

    @Override
    public @NotNull String getDisplayName() {
        return FluentQueryBundle.message("inspection.fetch.collection.pagination.display.name");
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
                if (terminal == null || !FluentQueryMethods.isCollectionFetchIncompatible(terminal)) {
                    return;
                }
                if (!FluentQueryChain.isFluentQueryFamilyCall(expression)
                        && !FluentQueryMethods.isKnownMethodName(terminal)) {
                    return;
                }
                String fetchCollection = FluentQueryChain.findInQualifierChain(
                        expression, FluentQueryMethods::isFetchCollectionMethod);
                if (fetchCollection == null) {
                    return;
                }
                holder.registerProblem(
                        expression.getMethodExpression().getReferenceNameElement() != null
                                ? expression.getMethodExpression().getReferenceNameElement()
                                : expression.getMethodExpression(),
                        FluentQueryBundle.message(
                                "inspection.fetch.collection.pagination.message",
                                fetchCollection,
                                terminal),
                        new RemoveFetchCollectionFromChainQuickFix());
            }
        };
    }
}
