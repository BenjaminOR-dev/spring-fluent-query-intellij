package dev.benjaminor.fluentquery.intellij.reference;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryCallAnalyzer;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryCallSite;
import org.jetbrains.annotations.NotNull;

/**
 * Creates path segment references for FluentQuery string literals.
 */
public final class FluentQueryPathReferenceProvider extends PsiReferenceProvider {

    @Override
    public PsiReference @NotNull [] getReferencesByElement(
            @NotNull PsiElement element, @NotNull ProcessingContext context) {
        if (!(element instanceof PsiLiteralExpression literal)) {
            return PsiReference.EMPTY_ARRAY;
        }
        if (!(literal.getValue() instanceof String)) {
            return PsiReference.EMPTY_ARRAY;
        }
        FluentQueryCallSite site = FluentQueryCallAnalyzer.analyze(literal);
        if (site == null) {
            return PsiReference.EMPTY_ARRAY;
        }
        return FluentQueryPathReference.forCallSite(site);
    }
}
