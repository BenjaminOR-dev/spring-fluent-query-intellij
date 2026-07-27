package dev.benjaminor.fluentquery.intellij.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryCallAnalyzer;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryCallSite;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryPathRole;
import dev.benjaminor.fluentquery.intellij.model.JpaProperty;
import dev.benjaminor.fluentquery.intellij.model.JpaPropertyKind;
import dev.benjaminor.fluentquery.intellij.model.PathResolver;
import dev.benjaminor.fluentquery.intellij.model.PathStrings;
import dev.benjaminor.fluentquery.intellij.model.PathSuggestionScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Autocomplete for FluentQuery path string literals (also covers empty / mid-token cases).
 */
public final class FluentQueryPathCompletionContributor extends CompletionContributor {

    public FluentQueryPathCompletionContributor() {
        extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement().inside(PsiLiteralExpression.class),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(
                            @NotNull CompletionParameters parameters,
                            @NotNull ProcessingContext context,
                            @NotNull CompletionResultSet result) {
                        PsiLiteralExpression literal = PsiTreeUtil.getParentOfType(
                                parameters.getPosition(), PsiLiteralExpression.class, false);
                        if (literal == null) {
                            literal = findLiteral(parameters.getPosition());
                        }
                        if (literal == null || !(literal.getValue() instanceof String)) {
                            return;
                        }

                        FluentQueryCallSite site = FluentQueryCallAnalyzer.analyze(literal);
                        if (site == null) {
                            return;
                        }

                        String prefix = prefixAtCaret(literal, parameters.getOffset(), site.pathText());

                        if (site.role() == FluentQueryPathRole.ASSOCIATION && prefix.indexOf(':') >= 0) {
                            return;
                        }

                        // select shorthand: after ':' complete scalar columns of the association leaf
                        if (site.role() == FluentQueryPathRole.SELECT && prefix.indexOf(':') >= 0) {
                            if (completeSelectShorthand(site, prefix, result)) {
                                result.stopHere();
                            }
                            return;
                        }

                        if (site.role() == FluentQueryPathRole.ATTRIBUTE && prefix.indexOf('.') >= 0) {
                            return;
                        }

                        PathSuggestionScope scope = suggestionScope(site.role(), prefix);
                        List<JpaProperty> props =
                                PathResolver.complete(site.entityType(), prefix, scope);
                        CompletionResultSet segmentResult =
                                result.withPrefixMatcher(segmentFragment(prefix));
                        for (JpaProperty p : props) {
                            segmentResult.addElement(lookup(p));
                        }
                        if (!props.isEmpty()) {
                            segmentResult.stopHere();
                        }
                    }
                });
    }

    /**
     * Nested property / select paths: allow associations while navigating; prefer scalars at the
     * root when there is no dot yet so {@code where}-like APIs stay clear. For the segment after a
     * trailing {@code '.'}, offer both nestable and scalar (ANY) so users can go deeper or finish.
     */
    private static @NotNull PathSuggestionScope suggestionScope(
            @NotNull FluentQueryPathRole role, @NotNull String prefix) {
        return switch (role) {
            case ASSOCIATION -> PathSuggestionScope.ASSOCIATIONS_ONLY;
            case ATTRIBUTE -> PathSuggestionScope.ATTRIBUTES_ONLY;
            case PROPERTY_PATH, SELECT -> PathSuggestionScope.ANY;
        };
    }

    private static boolean completeSelectShorthand(
            @NotNull FluentQueryCallSite site,
            @NotNull String prefix,
            @NotNull CompletionResultSet result) {
        int colon = prefix.indexOf(':');
        String assoc = prefix.substring(0, colon).trim();
        String after = prefix.substring(colon + 1);
        int lastComma = after.lastIndexOf(',');
        String colPrefix = lastComma >= 0 ? after.substring(lastComma + 1).trim() : after.trim();
        var resolved = PathResolver.resolve(site.entityType(), assoc, true);
        if (!resolved.isResolved() || resolved.tipEntity() == null) {
            return false;
        }
        List<JpaProperty> props = PathResolver.complete(
                resolved.tipEntity(), colPrefix, PathSuggestionScope.ATTRIBUTES_ONLY);
        CompletionResultSet segmentResult = result.withPrefixMatcher(colPrefix);
        for (JpaProperty p : props) {
            segmentResult.addElement(lookup(p));
        }
        return !props.isEmpty();
    }

    static @NotNull String segmentFragment(@NotNull String pathPrefix) {
        int lastDot = pathPrefix.lastIndexOf('.');
        return lastDot >= 0 ? pathPrefix.substring(lastDot + 1) : pathPrefix;
    }

    private static @NotNull LookupElementBuilder lookup(@NotNull JpaProperty property) {
        LookupElementBuilder builder = LookupElementBuilder.create(property.name())
                .withPresentableText(property.name())
                .withTypeText(typeText(property), true);
        if (property.kind().isAssociation()) {
            builder = builder.withBoldness(true);
        }
        return builder;
    }

    private static @NotNull String typeText(@NotNull JpaProperty property) {
        JpaPropertyKind kind = property.kind();
        String target = property.targetType() != null ? property.targetType().getName() : null;
        return switch (kind) {
            case ASSOCIATION_TO_ONE -> target != null ? "→ " + target : "assoc";
            case ASSOCIATION_TO_MANY -> target != null ? "⇉ " + target : "collection";
            case EMBEDDED -> target != null ? "embedded " + target : "embedded";
            case BASIC -> "attribute";
        };
    }

    private static @NotNull String prefixAtCaret(
            @NotNull PsiLiteralExpression literal, int absoluteOffset, @NotNull String pathText) {
        String text = literal.getText();
        int contentStart = text.startsWith("\"\"\"") ? 3 : 1;
        int relative = absoluteOffset - literal.getTextRange().getStartOffset() - contentStart;
        if (relative < 0) {
            return "";
        }
        String cleaned = PathStrings.stripCompletionDummy(pathText);
        if (relative > cleaned.length()) {
            String rawContent = text.length() > contentStart
                    ? text.substring(contentStart, Math.max(contentStart, text.length() - 1))
                    : "";
            String rawPrefix = relative <= rawContent.length()
                    ? rawContent.substring(0, relative)
                    : rawContent;
            return PathStrings.stripCompletionDummy(rawPrefix);
        }
        return cleaned.substring(0, relative);
    }

    private static @Nullable PsiLiteralExpression findLiteral(@NotNull PsiElement position) {
        PsiElement current = position;
        for (int i = 0; i < 6 && current != null; i++) {
            if (current instanceof PsiLiteralExpression lit) {
                return lit;
            }
            current = current.getParent();
        }
        return null;
    }
}
