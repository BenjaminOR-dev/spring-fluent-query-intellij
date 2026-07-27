package dev.benjaminor.fluentquery.intellij.model;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Detects FluentQuery path string literals and resolves the entity type in scope.
 */
public final class FluentQueryCallAnalyzer {

    private FluentQueryCallAnalyzer() {
    }

    public static @Nullable FluentQueryCallSite analyze(@NotNull PsiLiteralExpression literal) {
        Object value = literal.getValue();
        if (!(value instanceof String pathText)) {
            return null;
        }
        pathText = PathStrings.stripCompletionDummy(pathText);

        PsiExpressionList argList = PsiTreeUtil.getParentOfType(literal, PsiExpressionList.class, true);
        if (argList == null) {
            return null;
        }
        PsiElement parent = argList.getParent();
        if (!(parent instanceof PsiMethodCallExpression call)) {
            return null;
        }

        int argIndex = indexOf(argList.getExpressions(), literal);
        if (argIndex < 0) {
            return null;
        }

        PsiMethod method = call.resolveMethod();
        String methodName = method != null
                ? method.getName()
                : methodNameFromCall(call);
        if (methodName == null) {
            return null;
        }

        String containingFqcn = method != null && method.getContainingClass() != null
                ? method.getContainingClass().getQualifiedName()
                : null;

        // Resolved to a non-FluentQuery class → ignore (name collision).
        // Unresolved method → accept known FluentQuery API names as a heuristic.
        if (containingFqcn != null && !FluentQueryMethods.isFluentQueryFamily(containingFqcn)) {
            return null;
        }
        if (containingFqcn == null && !FluentQueryMethods.isKnownMethodName(methodName)) {
            return null;
        }

        FluentQueryPathRole role = FluentQueryMethods.roleFor(
                methodName, containingFqcn, argIndex, argList.getExpressionCount());
        if (role == null) {
            return null;
        }

        PsiClass entity = resolveEntityType(call, methodName, argList, argIndex, role, containingFqcn);
        if (entity == null) {
            return null;
        }

        return new FluentQueryCallSite(literal, call, role, entity, pathText, methodName);
    }

    private static int indexOf(PsiExpression @NotNull [] expressions, @NotNull PsiLiteralExpression literal) {
        for (int i = 0; i < expressions.length; i++) {
            if (expressions[i] == literal) {
                return i;
            }
        }
        return -1;
    }

    private static @Nullable String methodNameFromCall(@NotNull PsiMethodCallExpression call) {
        PsiReferenceExpression ref = call.getMethodExpression();
        return ref.getReferenceName();
    }

    private static @Nullable PsiClass resolveEntityType(
            @NotNull PsiMethodCallExpression call,
            @NotNull String methodName,
            @NotNull PsiExpressionList argList,
            int argIndex,
            @NotNull FluentQueryPathRole role,
            @Nullable String containingFqcn) {

        if (FluentQueryMethods.FQ_FETCH_REL.equals(containingFqcn)) {
            // FetchRel.of is usually nested in fetch(...) — try outer FluentQuery type
            PsiMethodCallExpression outer = PsiTreeUtil.getParentOfType(call, PsiMethodCallExpression.class, true);
            PsiClass fromOuter = outer != null ? entityFromFluentReceiver(outer) : null;
            if (fromOuter != null) {
                return fromOuter;
            }
        }

        // whereRelated*(relation, column): column resolves on leaf of relation path
        if (FluentQueryMethods.isRelationThenAttribute(methodName)
                && role == FluentQueryPathRole.ATTRIBUTE
                && argIndex == 1) {
            PsiClass root = entityFromFluentReceiver(call);
            if (root == null) {
                return null;
            }
            PsiExpression relArg = argList.getExpressions()[0];
            String relPath = stringValue(relArg);
            if (relPath == null || relPath.isBlank()) {
                return root;
            }
            PathResolveResult rel = PathResolver.resolve(root, relPath.trim(), true);
            return rel.isResolved() && rel.tipEntity() != null ? rel.tipEntity() : null;
        }

        if (FluentQueryMethods.FQ_RELATED_FILTER.equals(containingFqcn)
                || isRelatedFilterReceiver(call)) {
            return entityFromRelatedFilterContext(call);
        }

        return entityFromFluentReceiver(call);
    }

    private static boolean isRelatedFilterReceiver(@NotNull PsiMethodCallExpression call) {
        PsiType type = call.getMethodExpression().getQualifierExpression() != null
                ? call.getMethodExpression().getQualifierExpression().getType()
                : null;
        PsiClass cls = PsiUtil.resolveClassInType(type);
        return cls != null && FluentQueryMethods.FQ_RELATED_FILTER.equals(cls.getQualifiedName());
    }

    private static @Nullable PsiClass entityFromFluentReceiver(@NotNull PsiMethodCallExpression call) {
        PsiExpression qualifier = call.getMethodExpression().getQualifierExpression();
        if (qualifier == null) {
            return null;
        }

        // Direct FluentQuery<T> / chained call typed as FluentQuery<T>
        PsiClass fromType = entityTypeArgument(qualifier.getType());
        if (fromType != null) {
            return fromType;
        }

        // Walk qualifier chain: repo.query().where(...).fetch(...)
        PsiMethodCallExpression current = qualifier instanceof PsiMethodCallExpression m ? m : null;
        int guard = 0;
        while (current != null && guard++ < 32) {
            PsiClass fromCall = entityTypeArgument(current.getType());
            if (fromCall != null) {
                return fromCall;
            }
            String name = current.getMethodExpression().getReferenceName();
            if ("query".equals(name)) {
                PsiClass fromRepo = entityFromRepositoryQuery(current);
                if (fromRepo != null) {
                    return fromRepo;
                }
            }
            if ("of".equals(name)) {
                PsiClass fromOf = entityFromFluentQueryOf(current);
                if (fromOf != null) {
                    return fromOf;
                }
            }
            PsiExpression q = current.getMethodExpression().getQualifierExpression();
            current = q instanceof PsiMethodCallExpression m ? m : null;
        }

        // Variable typed FluentQueryRepository / FluentQuery
        if (qualifier instanceof PsiReferenceExpression ref) {
            PsiElement resolved = ref.resolve();
            if (resolved instanceof PsiVariable variable) {
                PsiClass fromVar = entityTypeArgument(variable.getType());
                if (fromVar != null) {
                    return fromVar;
                }
                PsiClass repoEntity = entityFromRepositoryType(variable.getType());
                if (repoEntity != null) {
                    return repoEntity;
                }
            }
        }

        return null;
    }

    private static @Nullable PsiClass entityFromRepositoryQuery(@NotNull PsiMethodCallExpression queryCall) {
        PsiExpression qualifier = queryCall.getMethodExpression().getQualifierExpression();
        if (qualifier == null) {
            return null;
        }
        PsiClass fromType = entityTypeArgument(qualifier.getType());
        if (fromType != null) {
            return fromType;
        }
        return entityFromRepositoryType(qualifier.getType());
    }

    private static @Nullable PsiClass entityFromFluentQueryOf(@NotNull PsiMethodCallExpression ofCall) {
        PsiExpression[] args = ofCall.getArgumentList().getExpressions();
        if (args.length == 0) {
            return null;
        }
        return entityTypeArgument(args[0].getType());
    }

    private static @Nullable PsiClass entityFromRelatedFilterContext(@NotNull PsiMethodCallExpression call) {
        // Lambda parameter: whereHas("books", f -> f.where(...))
        PsiExpression qualifier = call.getMethodExpression().getQualifierExpression();
        if (qualifier instanceof PsiReferenceExpression ref) {
            PsiElement resolved = ref.resolve();
            if (resolved instanceof PsiParameter) {
                PsiMethodCallExpression outer = findEnclosingAssociationCall(call);
                if (outer != null) {
                    return leafEntityFromAssociationCall(outer);
                }
            }
        }
        PsiMethodCallExpression outer = findEnclosingAssociationCall(call);
        if (outer != null) {
            return leafEntityFromAssociationCall(outer);
        }
        return entityFromFluentReceiver(call);
    }

    private static @Nullable PsiMethodCallExpression findEnclosingAssociationCall(
            @NotNull PsiElement from) {
        PsiMethodCallExpression current = PsiTreeUtil.getParentOfType(from, PsiMethodCallExpression.class, true);
        int guard = 0;
        while (current != null && guard++ < 24) {
            String name = current.getMethodExpression().getReferenceName();
            if (name != null && (FluentQueryMethods.roleFor(name, FluentQueryMethods.FQ_FLUENT_QUERY, 0, 1)
                    == FluentQueryPathRole.ASSOCIATION
                    || FluentQueryMethods.isRelationThenAttribute(name))) {
                // whereHas / fetch / whereRelated* with consumer
                return current;
            }
            current = PsiTreeUtil.getParentOfType(current, PsiMethodCallExpression.class, true);
        }
        return null;
    }

    private static @Nullable PsiClass leafEntityFromAssociationCall(@NotNull PsiMethodCallExpression outer) {
        PsiClass root = entityFromFluentReceiver(outer);
        if (root == null) {
            return null;
        }
        PsiExpression[] args = outer.getArgumentList().getExpressions();
        if (args.length == 0) {
            return root;
        }
        String relPath = stringValue(args[0]);
        if (relPath == null || relPath.isBlank()) {
            return root;
        }
        PathResolveResult rel = PathResolver.resolve(root, relPath.trim(), true);
        return rel.isResolved() && rel.tipEntity() != null ? rel.tipEntity() : null;
    }

    /**
     * Extracts {@code T} from {@code FluentQuery<T>}, {@code JpaSpecificationExecutor<T>}, etc.
     */
    private static @Nullable PsiClass entityTypeArgument(@Nullable PsiType type) {
        if (!(type instanceof PsiClassType classType)) {
            return null;
        }
        PsiClass resolved = classType.resolve();
        if (resolved == null) {
            return null;
        }
        String qn = resolved.getQualifiedName();
        if (FluentQueryMethods.FQ_FLUENT_QUERY.equals(qn)
                || "org.springframework.data.jpa.repository.JpaSpecificationExecutor".equals(qn)
                || "org.springframework.data.jpa.repository.JpaRepository".equals(qn)
                || FluentQueryMethods.FQ_REPOSITORY.equals(qn)) {
            PsiType[] params = classType.getParameters();
            if (params.length >= 1) {
                return PsiUtil.resolveClassInType(params[0]);
            }
        }
        // FluentQueryRepository subtype: walk supers for FluentQueryRepository<T,ID>
        PsiClass fromRepo = entityFromRepositoryClass(resolved, classType);
        if (fromRepo != null) {
            return fromRepo;
        }
        return null;
    }

    private static @Nullable PsiClass entityFromRepositoryType(@Nullable PsiType type) {
        if (!(type instanceof PsiClassType classType)) {
            return null;
        }
        PsiClass resolved = classType.resolve();
        if (resolved == null) {
            return null;
        }
        return entityFromRepositoryClass(resolved, classType);
    }

    private static @Nullable PsiClass entityFromRepositoryClass(
            @NotNull PsiClass psiClass, @NotNull PsiClassType siteType) {
        if (FluentQueryMethods.FQ_REPOSITORY.equals(psiClass.getQualifiedName())) {
            PsiType[] params = siteType.getParameters();
            if (params.length >= 1) {
                return PsiUtil.resolveClassInType(params[0]);
            }
        }
        for (PsiClassType st : psiClass.getSuperTypes()) {
            PsiClass sc = st.resolve();
            if (sc == null) {
                continue;
            }
            String sq = sc.getQualifiedName();
            if (FluentQueryMethods.FQ_REPOSITORY.equals(sq)
                    || "org.springframework.data.jpa.repository.JpaRepository".equals(sq)
                    || "org.springframework.data.jpa.repository.JpaSpecificationExecutor".equals(sq)) {
                PsiType[] params = st.getParameters();
                if (params.length >= 1) {
                    return PsiUtil.resolveClassInType(params[0]);
                }
            }
        }
        return null;
    }

    private static @Nullable String stringValue(@NotNull PsiExpression expression) {
        if (expression instanceof PsiLiteralExpression lit && lit.getValue() instanceof String s) {
            return PathStrings.stripCompletionDummy(s);
        }
        return null;
    }
}
