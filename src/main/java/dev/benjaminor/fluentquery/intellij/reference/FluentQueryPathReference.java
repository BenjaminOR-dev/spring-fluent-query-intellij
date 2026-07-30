package dev.benjaminor.fluentquery.intellij.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReferenceBase;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryCallSite;
import dev.benjaminor.fluentquery.intellij.model.FluentQueryPathRole;
import dev.benjaminor.fluentquery.intellij.model.PathResolveResult;
import dev.benjaminor.fluentquery.intellij.model.PathResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reference for one path segment inside a FluentQuery string literal.
 */
public final class FluentQueryPathReference extends PsiReferenceBase<PsiLiteralExpression> {

    private final @NotNull FluentQueryCallSite site;
    private final @NotNull String pathThroughSegment;
    private final int segmentIndex;
    private final boolean associationsOnly;

    public FluentQueryPathReference(
            @NotNull PsiLiteralExpression literal,
            @NotNull TextRange rangeInElement,
            @NotNull FluentQueryCallSite site,
            @NotNull String pathThroughSegment,
            int segmentIndex,
            boolean associationsOnly) {
        super(literal, rangeInElement, true);
        this.site = site;
        this.pathThroughSegment = pathThroughSegment;
        this.segmentIndex = segmentIndex;
        this.associationsOnly = associationsOnly;
    }

    @Override
    public @Nullable PsiElement resolve() {
        PathResolveResult result = PathResolver.resolve(
                site.entityType(), pathThroughSegment, associationsOnly);
        if (!result.isResolved() || segmentIndex >= result.segments().size()) {
            PathResolveResult soft = PathResolver.resolve(
                    site.entityType(), pathThroughSegment, associationsOnly || segmentIndex > 0);
            if (segmentIndex < soft.segments().size()) {
                return soft.segments().get(segmentIndex).navigationElement();
            }
            return null;
        }
        return result.segments().get(segmentIndex).navigationElement();
    }

    @Override
    public Object @NotNull [] getVariants() {
        return EMPTY_ARRAY;
    }

    public static FluentQueryPathReference @NotNull [] forCallSite(@NotNull FluentQueryCallSite site) {
        PsiLiteralExpression literal = site.literal();
        if (literal == null) {
            // Constant references: Ctrl+click already goes to the field; no in-string segments.
            return new FluentQueryPathReference[0];
        }
        String raw = site.pathText();
        int contentStart = contentStartOffset(literal);
        List<FluentQueryPathReference> refs = new ArrayList<>();

        if (site.role() == FluentQueryPathRole.SELECT && raw.indexOf(':') >= 0) {
            addSelectShorthandRefs(refs, literal, site, raw, contentStart);
            return refs.toArray(FluentQueryPathReference[]::new);
        }

        if (site.role() == FluentQueryPathRole.ATTRIBUTE && raw.indexOf('.') >= 0) {
            return new FluentQueryPathReference[0];
        }

        boolean associationsOnly = site.role() == FluentQueryPathRole.ASSOCIATION;
        addSegmentRefs(refs, literal, site, raw, contentStart, associationsOnly);
        return refs.toArray(FluentQueryPathReference[]::new);
    }

    private static void addSelectShorthandRefs(
            @NotNull List<FluentQueryPathReference> refs,
            @NotNull PsiLiteralExpression literal,
            @NotNull FluentQueryCallSite site,
            @NotNull String raw,
            int contentStart) {
        int colon = raw.indexOf(':');
        String assoc = raw.substring(0, colon).trim();
        int assocAt = raw.indexOf(assoc);
        if (!assoc.isEmpty() && assocAt >= 0) {
            addSegmentRefs(refs, literal, site, assoc, contentStart + assocAt, true);
        }
        String colsPart = raw.substring(colon + 1);
        int colsBase = contentStart + colon + 1;
        int offset = 0;
        for (String rawCol : colsPart.split(",", -1)) {
            String col = rawCol.trim();
            int leading = rawCol.indexOf(col);
            if (leading < 0) {
                leading = 0;
            }
            if (!col.isEmpty() && !assoc.isEmpty()) {
                String pathThrough = assoc + "." + col;
                TextRange range = TextRange.from(colsBase + offset + leading, col.length());
                // Column is the last segment of assoc.col — index = assocSegments
                int segmentIndex = assoc.isEmpty() ? 0 : assoc.split("\\.", -1).length;
                refs.add(new FluentQueryPathReference(
                        literal, range, site, pathThrough, segmentIndex, false));
            }
            offset += rawCol.length() + 1;
        }
    }

    private static void addSegmentRefs(
            @NotNull List<FluentQueryPathReference> refs,
            @NotNull PsiLiteralExpression literal,
            @NotNull FluentQueryCallSite site,
            @NotNull String path,
            int pathStartInElement,
            boolean associationsOnly) {
        if (path.isEmpty()) {
            refs.add(new FluentQueryPathReference(
                    literal,
                    TextRange.from(pathStartInElement, 0),
                    site,
                    "",
                    0,
                    associationsOnly));
            return;
        }

        String[] parts = path.split("\\.", -1);
        int offset = 0;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            TextRange range = TextRange.from(pathStartInElement + offset, part.length());
            String pathThrough = String.join(".", Arrays.copyOfRange(parts, 0, i + 1));
            boolean assocOnly = associationsOnly || i < parts.length - 1;
            refs.add(new FluentQueryPathReference(
                    literal, range, site, pathThrough, i, assocOnly));
            offset += part.length() + (i < parts.length - 1 ? 1 : 0);
        }
    }

    public static int contentStartOffset(@NotNull PsiLiteralExpression literal) {
        String text = literal.getText();
        if (text.startsWith("\"\"\"")) {
            return 3;
        }
        if (!text.isEmpty() && (text.charAt(0) == '"' || text.charAt(0) == '\'')) {
            return 1;
        }
        return 0;
    }
}
