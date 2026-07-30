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
 * Removes {@code fetchCollection}/{@code withCollection} from the chain
 * (keeps to-one {@code fetch}/{@code with}).
 */
public final class RemoveFetchCollectionFromChainQuickFix implements LocalQuickFix {

    @Override
    public @NotNull String getFamilyName() {
        return FluentQueryBundle.message("quickfix.remove.fetch.collection.family");
    }

    @Override
    public @NotNull String getName() {
        return FluentQueryBundle.message("quickfix.remove.fetch.collection.name");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        PsiMethodCallExpression terminal = findCall(descriptor.getPsiElement());
        if (terminal == null) {
            return;
        }
        java.util.ArrayList<PsiMethodCallExpression> targets = new java.util.ArrayList<>();
        PsiExpression current = terminal.getMethodExpression().getQualifierExpression();
        int guard = 0;
        while (current instanceof PsiMethodCallExpression chainCall && guard++ < 48) {
            String name = FluentQueryChain.resolveMethodName(chainCall);
            if (name != null && FluentQueryMethods.isFetchCollectionMethod(name)) {
                targets.add(chainCall);
            }
            current = chainCall.getMethodExpression().getQualifierExpression();
        }
        for (PsiMethodCallExpression target : targets) {
            if (!target.isValid()) {
                continue;
            }
            PsiExpression inner = target.getMethodExpression().getQualifierExpression();
            if (inner != null) {
                target.replace(inner.copy());
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
