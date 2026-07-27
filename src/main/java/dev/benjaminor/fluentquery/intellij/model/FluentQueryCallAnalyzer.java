package dev.benjaminor.fluentquery.intellij.model;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
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

        // new FetchRel("path", …)
        if (parent instanceof PsiNewExpression newExpr) {
            return analyzeFetchRelConstructor(literal, pathText, argList, newExpr);
        }

        if (!(parent instanceof PsiMethodCallExpression call)) {
            return null;
        }

        int argIndex = indexOf(argList.getExpressions(), literal);
        if (argIndex < 0) {
            return null;
        }

        // fetch(Map.of(...)) / Map.ofEntries(Map.entry(...)) — key is not a direct fetch arg
        if (isMapFactoryCall(call) || isMapEntryCall(call)) {
            return analyzeMapKeyLiteral(literal, pathText, call, argIndex);
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

        // User repository subtypes may override / re-export PropertyFilters methods.
        if (containingFqcn != null && !FluentQueryMethods.isFluentQueryFamily(containingFqcn)) {
            if (!FluentQueryMethods.isKnownMethodName(methodName)) {
                return null;
            }
            containingFqcn = null;
        } else if (containingFqcn == null && !FluentQueryMethods.isKnownMethodName(methodName)) {
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

    private static @Nullable FluentQueryCallSite analyzeFetchRelConstructor(
            @NotNull PsiLiteralExpression literal,
            @NotNull String pathText,
            @NotNull PsiExpressionList argList,
            @NotNull PsiNewExpression newExpr) {
        if (indexOf(argList.getExpressions(), literal) != 0) {
            return null;
        }
        PsiJavaCodeReferenceElement classRef = newExpr.getClassReference();
        if (classRef == null) {
            return null;
        }
        PsiElement resolved = classRef.resolve();
        PsiClass psiClass = resolved instanceof PsiClass c ? c : null;
        if (psiClass == null
                || !FluentQueryMethods.FQ_FETCH_REL.equals(psiClass.getQualifiedName())) {
            return null;
        }
        PsiMethodCallExpression outer =
                PsiTreeUtil.getParentOfType(newExpr, PsiMethodCallExpression.class, true);
        if (outer == null) {
            return null;
        }
        PsiClass entity = entityFromFluentReceiver(outer);
        if (entity == null) {
            return null;
        }
        String methodName = outer.getMethodExpression().getReferenceName();
        if (methodName == null) {
            methodName = "fetch";
        }
        return new FluentQueryCallSite(
                literal, outer, FluentQueryPathRole.ASSOCIATION, entity, pathText, methodName);
    }

    private static @Nullable FluentQueryCallSite analyzeMapKeyLiteral(
            @NotNull PsiLiteralExpression literal,
            @NotNull String pathText,
            @NotNull PsiMethodCallExpression mapCall,
            int argIndex) {
        String mapMethod = mapCall.getMethodExpression().getReferenceName();
        if ("entry".equals(mapMethod) && argIndex != 0) {
            return null;
        }
        // Map.of(k, v, k, v, …) — keys are even indices
        if ("of".equals(mapMethod) && (argIndex % 2) != 0) {
            return null;
        }
        PsiMethodCallExpression outer = findEnclosingAssociationCall(mapCall);
        if (outer == null) {
            return null;
        }
        PsiClass entity = entityFromFluentReceiver(outer);
        if (entity == null) {
            return null;
        }
        String methodName = outer.getMethodExpression().getReferenceName();
        if (methodName == null) {
            return null;
        }
        return new FluentQueryCallSite(
                literal, outer, FluentQueryPathRole.ASSOCIATION, entity, pathText, methodName);
    }

    private static boolean isMapFactoryCall(@NotNull PsiMethodCallExpression call) {
        String name = call.getMethodExpression().getReferenceName();
        if (name == null || !FluentQueryMethods.isMapFactoryMethod(name)) {
            return false;
        }
        PsiMethod method = call.resolveMethod();
        if (method != null && method.getContainingClass() != null) {
            String qn = method.getContainingClass().getQualifiedName();
            return "java.util.Map".equals(qn)
                    || "java.util.Collections".equals(qn)
                    || (qn != null && qn.endsWith(".ImmutableMap"));
        }
        return findEnclosingAssociationCall(call) != null;
    }

    private static boolean isMapEntryCall(@NotNull PsiMethodCallExpression call) {
        if (!"entry".equals(call.getMethodExpression().getReferenceName())) {
            return false;
        }
        PsiMethod method = call.resolveMethod();
        if (method != null && method.getContainingClass() != null) {
            String qn = method.getContainingClass().getQualifiedName();
            if (!"java.util.Map".equals(qn)) {
                return false;
            }
        }
        // Only treat as fetch path when nested under Map.ofEntries → fetch/with
        PsiMethodCallExpression parent =
                PsiTreeUtil.getParentOfType(call, PsiMethodCallExpression.class, true);
        return parent != null && isMapFactoryCall(parent);
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
        return call.getMethodExpression().getReferenceName();
    }

    private static @Nullable PsiClass resolveEntityType(
            @NotNull PsiMethodCallExpression call,
            @NotNull String methodName,
            @NotNull PsiExpressionList argList,
            int argIndex,
            @NotNull FluentQueryPathRole role,
            @Nullable String containingFqcn) {

        if (FluentQueryMethods.FQ_FETCH_REL.equals(containingFqcn)) {
            PsiMethodCallExpression outer =
                    PsiTreeUtil.getParentOfType(call, PsiMethodCallExpression.class, true);
            PsiClass fromOuter = outer != null ? entityFromFluentReceiver(outer) : null;
            if (fromOuter != null) {
                return fromOuter;
            }
        }

        if (FluentQueryMethods.isRelationThenAttribute(methodName)
                && role == FluentQueryPathRole.ATTRIBUTE
                && argIndex == 1) {
            PsiClass root = entityFromFluentReceiver(call);
            if (root == null) {
                // PropertyFilters may be invoked on the repository directly
                root = entityFromRepositoryReceiver(call);
            }
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

        if (FluentQueryMethods.FQ_PROPERTY_FILTERS.equals(containingFqcn)) {
            PsiClass fromRepo = entityFromRepositoryReceiver(call);
            if (fromRepo != null) {
                return fromRepo;
            }
        }

        PsiClass fromFluent = entityFromFluentReceiver(call);
        if (fromFluent != null) {
            return fromFluent;
        }
        return entityFromRepositoryReceiver(call);
    }

    private static @Nullable PsiClass entityFromRepositoryReceiver(@NotNull PsiMethodCallExpression call) {
        PsiExpression qualifier = call.getMethodExpression().getQualifierExpression();
        if (qualifier == null) {
            return null;
        }
        PsiClass fromType = entityTypeArgument(qualifier.getType());
        if (fromType != null) {
            return fromType;
        }
        return entityFromRepositoryType(qualifier.getType());
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

        PsiClass fromType = entityTypeArgument(qualifier.getType());
        if (fromType != null) {
            return fromType;
        }

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
        PsiMethodCallExpression current =
                PsiTreeUtil.getParentOfType(from, PsiMethodCallExpression.class, true);
        int guard = 0;
        while (current != null && guard++ < 24) {
            String name = current.getMethodExpression().getReferenceName();
            if (name != null && (FluentQueryMethods.isAssociationMethodName(name)
                    || FluentQueryMethods.isRelationThenAttribute(name))) {
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
                || FluentQueryMethods.FQ_REPOSITORY.equals(qn)
                || FluentQueryMethods.FQ_PROPERTY_FILTERS.equals(qn)) {
            PsiType[] params = classType.getParameters();
            if (params.length >= 1) {
                return PsiUtil.resolveClassInType(params[0]);
            }
        }
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
        java.util.ArrayDeque<PsiClassType> queue = new java.util.ArrayDeque<>();
        queue.add(siteType);
        java.util.HashSet<PsiClass> visited = new java.util.HashSet<>();
        int guard = 0;
        while (!queue.isEmpty() && guard++ < 64) {
            PsiClassType current = queue.poll();
            var resolveResult = current.resolveGenerics();
            PsiClass resolved = resolveResult.getElement();
            if (resolved == null || !visited.add(resolved)) {
                continue;
            }
            String qn = resolved.getQualifiedName();
            if (isEntityTypeHost(qn)) {
                PsiType[] params = current.getParameters();
                if (params.length >= 1) {
                    PsiClass entity = PsiUtil.resolveClassInType(params[0]);
                    if (entity != null) {
                        return entity;
                    }
                }
            }
            var substitutor = resolveResult.getSubstitutor();
            for (PsiClassType superType : resolved.getSuperTypes()) {
                PsiType substituted = substitutor.substitute(superType);
                if (substituted instanceof PsiClassType sct) {
                    queue.add(sct);
                }
            }
        }
        // Fallback: bare PsiClass without usable site generics (rare)
        if (FluentQueryMethods.FQ_REPOSITORY.equals(psiClass.getQualifiedName())
                || FluentQueryMethods.FQ_PROPERTY_FILTERS.equals(psiClass.getQualifiedName())) {
            PsiType[] params = siteType.getParameters();
            if (params.length >= 1) {
                return PsiUtil.resolveClassInType(params[0]);
            }
        }
        return null;
    }

    private static boolean isEntityTypeHost(@Nullable String qualifiedName) {
        return FluentQueryMethods.FQ_REPOSITORY.equals(qualifiedName)
                || FluentQueryMethods.FQ_PROPERTY_FILTERS.equals(qualifiedName)
                || FluentQueryMethods.FQ_FLUENT_QUERY.equals(qualifiedName)
                || "org.springframework.data.jpa.repository.JpaRepository".equals(qualifiedName)
                || "org.springframework.data.jpa.repository.JpaSpecificationExecutor".equals(qualifiedName);
    }

    private static @Nullable String stringValue(@NotNull PsiExpression expression) {
        if (expression instanceof PsiLiteralExpression lit && lit.getValue() instanceof String s) {
            return PathStrings.stripCompletionDummy(s);
        }
        return null;
    }
}
