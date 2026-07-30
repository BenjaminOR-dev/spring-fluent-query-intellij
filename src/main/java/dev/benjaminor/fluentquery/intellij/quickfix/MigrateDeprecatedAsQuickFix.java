package dev.benjaminor.fluentquery.intellij.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import dev.benjaminor.fluentquery.intellij.FluentQueryBundle;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryChain;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryMethods;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Migrates deprecated {@code *As} terminals to Class overloads
 * ({@code firstAs}→{@code first}, {@code pageAs(Class, pageable)}→{@code page(pageable, Class)}, …).
 */
public final class MigrateDeprecatedAsQuickFix implements LocalQuickFix {

    @Override
    public @NotNull String getFamilyName() {
        return FluentQueryBundle.message("quickfix.migrate.as.family");
    }

    @Override
    public @NotNull String getName() {
        return FluentQueryBundle.message("quickfix.migrate.as.name");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        PsiMethodCallExpression call = findCall(descriptor.getPsiElement());
        if (call == null) {
            return;
        }
        String asName = FluentQueryChain.resolveMethodName(call);
        if (asName == null) {
            return;
        }
        String preferred = FluentQueryMethods.preferredNameForDeprecatedAs(asName);
        if (preferred == null) {
            return;
        }

        PsiExpression qualifier = call.getMethodExpression().getQualifierExpression();
        PsiExpressionList args = call.getArgumentList();
        PsiExpression[] expressions = args.getExpressions();
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

        String argText;
        if (FluentQueryMethods.deprecatedAsNeedsArgReorder(asName) && expressions.length >= 2) {
            if ("paginateAs".equals(asName) && expressions.length >= 3) {
                // paginateAs(Class, page, size) → paginate(page, size, Class)
                argText = expressions[1].getText() + ", " + expressions[2].getText()
                        + ", " + expressions[0].getText();
            } else {
                // pageAs/sliceAs(Class, pageable) → page/slice(pageable, Class)
                argText = expressions[1].getText() + ", " + expressions[0].getText();
            }
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < expressions.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(expressions[i].getText());
            }
            argText = sb.toString();
        }

        String qualifierText = qualifier != null ? qualifier.getText() : "";
        String replacement = qualifierText + "." + preferred + "(" + argText + ")";
        PsiExpression newExpr = factory.createExpressionFromText(replacement, call);
        call.replace(newExpr);
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
