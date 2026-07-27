package dev.benjaminor.fluentquery.intellij.reference;

import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import org.jetbrains.annotations.NotNull;

/**
 * Registers path references inside FluentQuery string literals
 * ({@code where}, {@code select}, {@code fetch}, …).
 *
 * <p>Scaffold only — providers will be added in the MVP.
 */
public final class FluentQueryPathReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        // TODO: register PsiReferenceProvider for FluentQuery path string arguments
    }
}
