package dev.benjaminor.fluentquery.intellij.model;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Attribute / association map for one JPA entity (or embeddable) class.
 */
public final class JpaEntityGraph {

    private final @NotNull PsiClass entityClass;
    private final @NotNull Map<String, JpaProperty> properties;

    public JpaEntityGraph(@NotNull PsiClass entityClass, @NotNull Map<String, JpaProperty> properties) {
        this.entityClass = entityClass;
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }

    public @NotNull PsiClass entityClass() {
        return entityClass;
    }

    public @Nullable JpaProperty find(@NotNull String name) {
        return properties.get(name);
    }

    public @NotNull Collection<JpaProperty> properties() {
        return properties.values();
    }

    public @NotNull Collection<JpaProperty> associations() {
        return properties.values().stream().filter(p -> p.kind().isAssociation()).toList();
    }

    public @NotNull Collection<JpaProperty> nestable() {
        return properties.values().stream().filter(p -> p.kind().canNest()).toList();
    }
}
