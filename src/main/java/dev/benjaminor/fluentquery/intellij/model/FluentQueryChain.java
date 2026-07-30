package dev.benjaminor.fluentquery.intellij.model;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Walk FluentQuery method-call qualifier chains (fetch → select → first, …).
 */
public final class FluentQueryChain {

    private FluentQueryChain() {
    }

    public static boolean isFluentQueryFamilyCall(@NotNull PsiMethodCallExpression call) {
        PsiMethod method = call.resolveMethod();
        if (method != null && method.getContainingClass() != null) {
            String fqcn = method.getContainingClass().getQualifiedName();
            return FluentQueryMethods.FQ_FLUENT_QUERY.equals(fqcn)
                    || FluentQueryMethods.isFluentQueryFamily(fqcn);
        }
        String name = call.getMethodExpression().getReferenceName();
        return name != null && FluentQueryMethods.isKnownMethodName(name);
    }

    public static @Nullable String resolveMethodName(@NotNull PsiMethodCallExpression call) {
        PsiMethod method = call.resolveMethod();
        if (method != null) {
            return method.getName();
        }
        return call.getMethodExpression().getReferenceName();
    }

    /**
     * Walks {@code call}'s qualifier chain (not including {@code call} itself)
     * and returns the first method name matching {@code predicate}.
     */
    public static @Nullable String findInQualifierChain(
            @NotNull PsiMethodCallExpression call, @NotNull Predicate<String> predicate) {
        PsiExpression current = call.getMethodExpression().getQualifierExpression();
        while (current instanceof PsiMethodCallExpression chainCall) {
            String name = resolveMethodName(chainCall);
            if (name != null && predicate.test(name)) {
                return name;
            }
            current = chainCall.getMethodExpression().getQualifierExpression();
        }
        return null;
    }

    public static @Nullable String findInQualifierChain(
            @NotNull PsiMethodCallExpression call, @NotNull Set<String> names) {
        return findInQualifierChain(call, names::contains);
    }

    /**
     * All method calls in the chain including {@code call}, outermost first
     * ({@code query().fetch().select().first()} → first, select, fetch, query).
     */
    public static @NotNull List<PsiMethodCallExpression> includingQualifiers(
            @NotNull PsiMethodCallExpression call) {
        List<PsiMethodCallExpression> out = new ArrayList<>();
        PsiExpression current = call;
        int guard = 0;
        while (current instanceof PsiMethodCallExpression chainCall && guard++ < 48) {
            out.add(chainCall);
            current = chainCall.getMethodExpression().getQualifierExpression();
        }
        return out;
    }

    public static boolean qualifierIsVariableOrQuery(@Nullable PsiExpression qualifier) {
        return qualifier instanceof PsiReferenceExpression
                || (qualifier instanceof PsiMethodCallExpression q
                && "query".equals(q.getMethodExpression().getReferenceName()));
    }
}
