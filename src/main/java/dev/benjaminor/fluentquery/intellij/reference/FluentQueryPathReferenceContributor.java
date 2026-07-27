package dev.benjaminor.fluentquery.intellij.reference;

import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import org.jetbrains.annotations.NotNull;

/**
 * Registers path references inside FluentQuery string literals
 * ({@code where}, {@code select}, {@code fetch}, …).
 */
public final class FluentQueryPathReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                PsiJavaPatterns.psiElement(PsiLiteralExpression.class),
                new FluentQueryPathReferenceProvider(),
                PsiReferenceRegistrar.DEFAULT_PRIORITY);
    }
}
