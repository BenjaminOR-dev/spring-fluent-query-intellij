package dev.benjaminor.fluentquery.intellij.model;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Builds a {@link JpaEntityGraph} from a {@link PsiClass} by reading fields and
 * property getters with Jakarta / Javax Persistence annotations.
 *
 * <p>Results are cached on the {@link PsiClass} and invalidated when it changes.
 */
public final class JpaEntityGraphResolver {

    private static final Set<String> TO_ONE = Set.of(
            "ManyToOne", "OneToOne",
            "jakarta.persistence.ManyToOne", "jakarta.persistence.OneToOne",
            "javax.persistence.ManyToOne", "javax.persistence.OneToOne");

    private static final Set<String> TO_MANY = Set.of(
            "OneToMany", "ManyToMany",
            "jakarta.persistence.OneToMany", "jakarta.persistence.ManyToMany",
            "javax.persistence.OneToMany", "javax.persistence.ManyToMany");

    private static final Set<String> EMBEDDED = Set.of(
            "Embedded", "jakarta.persistence.Embedded", "javax.persistence.Embedded");

    private static final Set<String> TRANSIENT = Set.of(
            "Transient", "jakarta.persistence.Transient", "javax.persistence.Transient");

    private JpaEntityGraphResolver() {
    }

    public static @NotNull JpaEntityGraph resolve(@NotNull PsiClass entityClass) {
        return CachedValuesManager.getCachedValue(entityClass, () ->
                CachedValueProvider.Result.create(build(entityClass), entityClass));
    }

    private static @NotNull JpaEntityGraph build(@NotNull PsiClass entityClass) {
        Map<String, JpaProperty> props = new LinkedHashMap<>();
        for (PsiField field : entityClass.getAllFields()) {
            if (!isPersistentField(field)) {
                continue;
            }
            String name = field.getName();
            if (name == null || props.containsKey(name)) {
                continue;
            }
            props.put(name, propertyFrom(name, field.getType(), field.getAnnotations(), field));
        }
        // Property-access: mapping annotations on getters (do not invent props from every getX)
        for (PsiMethod method : entityClass.getAllMethods()) {
            if (!isPersistentGetter(method) || !hasMappingAnnotation(method.getAnnotations())) {
                continue;
            }
            String name = PropertyUtilBase.getPropertyName(method);
            if (name == null || props.containsKey(name)) {
                continue;
            }
            PsiType returnType = method.getReturnType();
            if (returnType == null) {
                continue;
            }
            props.put(name, propertyFrom(name, returnType, method.getAnnotations(), method));
        }
        return new JpaEntityGraph(entityClass, props);
    }

    private static @NotNull JpaProperty propertyFrom(
            @NotNull String name,
            @NotNull PsiType type,
            PsiAnnotation @NotNull [] annotations,
            @NotNull com.intellij.psi.PsiElement navigation) {
        JpaPropertyKind kind = kindOf(type, annotations);
        PsiClass target = switch (kind) {
            case ASSOCIATION_TO_ONE, EMBEDDED -> PsiUtil.resolveClassInType(type);
            case ASSOCIATION_TO_MANY -> collectionElementClass(type);
            case BASIC -> null;
        };
        return new JpaProperty(name, kind, target, navigation);
    }

    private static boolean isPersistentField(@NotNull PsiField field) {
        if (field.hasModifierProperty(PsiModifier.STATIC)
                || field.hasModifierProperty(PsiModifier.TRANSIENT)) {
            return false;
        }
        String name = field.getName();
        if ("serialVersionUID".equals(name)) {
            return false;
        }
        return !hasTransientAnnotation(field.getAnnotations());
    }

    private static final Set<String> MAPPING = Set.of(
            "Id", "Column", "Basic", "Version", "EmbeddedId",
            "ManyToOne", "OneToOne", "OneToMany", "ManyToMany", "Embedded",
            "JoinColumn", "JoinTable", "Enumerated", "Temporal", "Lob",
            "jakarta.persistence.Id", "jakarta.persistence.Column", "jakarta.persistence.Basic",
            "jakarta.persistence.Version", "jakarta.persistence.EmbeddedId",
            "jakarta.persistence.ManyToOne", "jakarta.persistence.OneToOne",
            "jakarta.persistence.OneToMany", "jakarta.persistence.ManyToMany",
            "jakarta.persistence.Embedded", "jakarta.persistence.JoinColumn",
            "javax.persistence.Id", "javax.persistence.Column", "javax.persistence.Basic",
            "javax.persistence.ManyToOne", "javax.persistence.OneToOne",
            "javax.persistence.OneToMany", "javax.persistence.ManyToMany",
            "javax.persistence.Embedded");

    private static boolean hasMappingAnnotation(PsiAnnotation @NotNull [] annotations) {
        for (PsiAnnotation ann : annotations) {
            String qn = ann.getQualifiedName();
            if (qn != null && (MAPPING.contains(qn) || MAPPING.contains(shortName(qn)))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPersistentGetter(@NotNull PsiMethod method) {
        if (method.isConstructor()
                || method.hasModifierProperty(PsiModifier.STATIC)
                || method.getParameterList().getParametersCount() != 0) {
            return false;
        }
        PsiClass owner = method.getContainingClass();
        if (owner != null && "java.lang.Object".equals(owner.getQualifiedName())) {
            return false;
        }
        String name = method.getName();
        if (name == null || !(name.startsWith("get") || name.startsWith("is"))) {
            return false;
        }
        if (PropertyUtilBase.getPropertyName(method) == null) {
            return false;
        }
        return !hasTransientAnnotation(method.getAnnotations());
    }

    private static boolean hasTransientAnnotation(PsiAnnotation @NotNull [] annotations) {
        for (PsiAnnotation ann : annotations) {
            String qn = ann.getQualifiedName();
            if (qn != null && (TRANSIENT.contains(qn) || TRANSIENT.contains(shortName(qn)))) {
                return true;
            }
        }
        return false;
    }

    private static @NotNull JpaPropertyKind kindOf(
            @NotNull PsiType type, PsiAnnotation @NotNull [] annotations) {
        for (PsiAnnotation ann : annotations) {
            String qn = ann.getQualifiedName();
            if (qn == null) {
                continue;
            }
            if (TO_ONE.contains(qn) || TO_ONE.contains(shortName(qn))) {
                return JpaPropertyKind.ASSOCIATION_TO_ONE;
            }
            if (TO_MANY.contains(qn) || TO_MANY.contains(shortName(qn))) {
                return JpaPropertyKind.ASSOCIATION_TO_MANY;
            }
            if (EMBEDDED.contains(qn) || EMBEDDED.contains(shortName(qn))) {
                return JpaPropertyKind.EMBEDDED;
            }
        }
        PsiClass resolved = PsiUtil.resolveClassInType(type);
        if (resolved != null && hasEntityAnnotation(resolved)) {
            return JpaPropertyKind.ASSOCIATION_TO_ONE;
        }
        PsiClass element = collectionElementClass(type);
        if (element != null && hasEntityAnnotation(element)) {
            return JpaPropertyKind.ASSOCIATION_TO_MANY;
        }
        if (resolved != null && hasEmbeddableAnnotation(resolved)) {
            return JpaPropertyKind.EMBEDDED;
        }
        return JpaPropertyKind.BASIC;
    }

    private static boolean hasEntityAnnotation(@NotNull PsiClass type) {
        return hasAnnotation(type, "Entity", "jakarta.persistence.Entity", "javax.persistence.Entity");
    }

    private static boolean hasEmbeddableAnnotation(@NotNull PsiClass type) {
        return hasAnnotation(type, "Embeddable", "jakarta.persistence.Embeddable", "javax.persistence.Embeddable");
    }

    private static boolean hasAnnotation(@NotNull PsiClass type, String @NotNull ... names) {
        Set<String> wanted = Set.of(names);
        for (PsiAnnotation ann : type.getAnnotations()) {
            String qn = ann.getQualifiedName();
            if (qn != null && (wanted.contains(qn) || wanted.contains(shortName(qn)))) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable PsiClass collectionElementClass(@NotNull PsiType type) {
        PsiClass raw = PsiUtil.resolveClassInType(type);
        if (raw == null) {
            return null;
        }
        String qn = raw.getQualifiedName();
        if (qn == null) {
            return null;
        }
        if (!(qn.startsWith("java.util.") || "java.lang.Iterable".equals(qn))) {
            if (!isCollectionName(raw.getName())) {
                return null;
            }
        }
        PsiType[] params = type instanceof com.intellij.psi.PsiClassType ct
                ? ct.getParameters()
                : PsiType.EMPTY_ARRAY;
        if (params.length != 1) {
            return null;
        }
        return PsiUtil.resolveClassInType(params[0]);
    }

    private static boolean isCollectionName(@Nullable String name) {
        return "List".equals(name) || "Set".equals(name) || "Collection".equals(name)
                || "Iterable".equals(name);
    }

    private static @NotNull String shortName(@Nullable String qn) {
        if (qn == null) {
            return "";
        }
        int dot = qn.lastIndexOf('.');
        return dot >= 0 ? qn.substring(dot + 1) : qn;
    }
}
