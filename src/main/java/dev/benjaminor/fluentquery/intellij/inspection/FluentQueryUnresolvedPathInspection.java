package dev.benjaminor.fluentquery.intellij.inspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReferenceExpression;
import dev.benjaminor.fluentquery.intellij.FluentQueryBundle;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryCallAnalyzer;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryCallSite;
import dev.benjaminor.fluentquery.intellij.model.PathValidator;
import dev.benjaminor.fluentquery.intellij.reference.FluentQueryPathReference;
import org.jetbrains.annotations.NotNull;

/**
 * Highlights FluentQuery path segments that do not resolve to an entity field or association.
 * Supports inline string literals and {@code static final String} constant references.
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
                report(expression);
            }

            @Override
            public void visitReferenceExpression(@NotNull PsiReferenceExpression expression) {
                // Only path args that resolve to static final String constants
                FluentQueryCallSite site = FluentQueryCallAnalyzer.analyzePathExpression(expression);
                if (site == null || site.isInlineLiteral()) {
                    return;
                }
                reportIssues(site, expression, true);
            }

            private void report(@NotNull PsiLiteralExpression expression) {
                FluentQueryCallSite site = FluentQueryCallAnalyzer.analyze(expression);
                if (site == null) {
                    return;
                }
                reportIssues(site, expression, false);
            }

            private void reportIssues(
                    @NotNull FluentQueryCallSite site,
                    @NotNull PsiExpression anchor,
                    boolean wholeElement) {
                for (PathValidator.Issue issue : PathValidator.validate(site)) {
                    if (wholeElement || site.literal() == null) {
                        holder.registerProblem(
                                anchor,
                                FluentQueryBundle.message(issue.messageKey(), issue.messageArgs()));
                        continue;
                    }
                    PsiLiteralExpression literal = site.literal();
                    int contentStart = FluentQueryPathReference.contentStartOffset(literal);
                    String text = literal.getText();
                    int max = Math.max(0, text.length());
                    int start = Math.min(contentStart + issue.start(), max);
                    int end = Math.min(contentStart + issue.end(), max);
                    if (end < start) {
                        end = start;
                    }
                    holder.registerProblem(
                            literal,
                            TextRange.create(start, end),
                            FluentQueryBundle.message(issue.messageKey(), issue.messageArgs()));
                }
            }
        };
    }
}
