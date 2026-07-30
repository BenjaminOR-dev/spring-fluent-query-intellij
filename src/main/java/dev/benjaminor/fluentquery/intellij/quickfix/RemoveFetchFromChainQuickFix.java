package dev.benjaminor.fluentquery.intellij.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import dev.benjaminor.fluentquery.intellij.FluentQueryBundle;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryChain;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryMethods;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Removes every {@code fetch}/{@code with}* call from the qualifier chain of the highlighted terminal.
 */
public final class RemoveFetchFromChainQuickFix implements LocalQuickFix {

    @Override
    public @NotNull String getFamilyName() {
        return FluentQueryBundle.message("quickfix.remove.fetch.family");
    }

    @Override
    public @NotNull String getName() {
        return FluentQueryBundle.message("quickfix.remove.fetch.name");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        PsiMethodCallExpression terminal = findCall(descriptor.getPsiElement());
        if (terminal == null) {
            return;
        }
        // Collect fetch calls innermost-first so replacements stay valid
        java.util.ArrayList<PsiMethodCallExpression> fetches = new java.util.ArrayList<>();
        PsiExpression current = terminal.getMethodExpression().getQualifierExpression();
        int guard = 0;
        while (current instanceof PsiMethodCallExpression chainCall && guard++ < 48) {
            String name = FluentQueryChain.resolveMethodName(chainCall);
            if (name != null && FluentQueryMethods.isFetchChainMethod(name)) {
                fetches.add(chainCall);
            }
            current = chainCall.getMethodExpression().getQualifierExpression();
        }
        for (PsiMethodCallExpression fetchCall : fetches) {
            if (!fetchCall.isValid()) {
                continue;
            }
            PsiExpression inner = fetchCall.getMethodExpression().getQualifierExpression();
            if (inner != null) {
                fetchCall.replace(inner.copy());
            }
        }
    }

    private static @Nullable PsiMethodCallExpression findCall(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }
        if (element instanceof PsiMethodCallExpression call) {
            return call;
        }
        PsiElement parent = element.getParent();
        if (parent instanceof PsiReferenceExpression && parent.getParent() instanceof PsiMethodCallExpression call) {
            return call;
        }
        if (parent instanceof PsiMethodCallExpression call) {
            return call;
        }
        return null;
    }
}
