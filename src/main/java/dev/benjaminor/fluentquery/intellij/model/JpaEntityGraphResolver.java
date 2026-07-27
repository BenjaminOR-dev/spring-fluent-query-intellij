package dev.benjaminor.fluentquery.intellij.model;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Builds a {@link JpaEntityGraph} from a {@link PsiClass} by reading fields and
 * Jakarta / Javax Persistence annotations (matched by simple or FQCN name).
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
            JpaPropertyKind kind = kindOf(field);
            PsiClass target = switch (kind) {
                case ASSOCIATION_TO_ONE, EMBEDDED -> PsiUtil.resolveClassInType(field.getType());
                case ASSOCIATION_TO_MANY -> collectionElementClass(field.getType());
                case BASIC -> null;
            };
            props.put(name, new JpaProperty(name, kind, target, field));
        }
        return new JpaEntityGraph(entityClass, props);
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
        for (PsiAnnotation ann : field.getAnnotations()) {
            String qn = ann.getQualifiedName();
            if (qn != null && (TRANSIENT.contains(qn) || TRANSIENT.contains(shortName(qn)))) {
                return false;
            }
        }
        return true;
    }

    private static @NotNull JpaPropertyKind kindOf(@NotNull PsiField field) {
        for (PsiAnnotation ann : field.getAnnotations()) {
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
        PsiClass type = PsiUtil.resolveClassInType(field.getType());
        if (type != null && hasEntityAnnotation(type)) {
            return JpaPropertyKind.ASSOCIATION_TO_ONE;
        }
        PsiClass element = collectionElementClass(field.getType());
        if (element != null && hasEntityAnnotation(element)) {
            return JpaPropertyKind.ASSOCIATION_TO_MANY;
        }
        if (type != null && hasEmbeddableAnnotation(type)) {
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
