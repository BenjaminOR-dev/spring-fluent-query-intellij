package dev.benjaminor.fluentquery.intellij.inspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiLiteralExpression;
import dev.benjaminor.fluentquery.intellij.FluentQueryBundle;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryCallAnalyzer;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryCallSite;
import dev.benjaminor.fluentquery.intellij.model.PathValidator;
import dev.benjaminor.fluentquery.intellij.reference.FluentQueryPathReference;
import org.jetbrains.annotations.NotNull;

/**
 * Highlights FluentQuery path segments that do not resolve to an entity field or association.
 */
public final class FluentQueryUnresolvedPathInspection extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public @NotNull String getShortName() {
        return "FluentQueryUnresolvedPath";
    }

    @Override
    public @NotNull String getDisplayName() {
        return FluentQueryBundle.message("inspection.unresolved.path.display.name");
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
            public void visitLiteralExpression(@NotNull PsiLiteralExpression expression) {
                FluentQueryCallSite site = FluentQueryCallAnalyzer.analyze(expression);
                if (site == null) {
                    return;
                }
                int contentStart = FluentQueryPathReference.contentStartOffset(expression);
                String text = expression.getText();
                int max = Math.max(0, text.length());
                for (PathValidator.Issue issue : PathValidator.validate(site)) {
                    int start = Math.min(contentStart + issue.start(), max);
                    int end = Math.min(contentStart + issue.end(), max);
                    if (end < start) {
                        end = start;
                    }
                    TextRange rangeInElement = TextRange.create(start, end);
                    holder.registerProblem(
                            expression,
                            rangeInElement,
                            FluentQueryBundle.message(issue.messageKey(), issue.messageArgs()));
                }
            }
        };
    }
}
