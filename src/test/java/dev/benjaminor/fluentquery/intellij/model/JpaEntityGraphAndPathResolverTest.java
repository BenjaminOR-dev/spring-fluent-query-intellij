package dev.benjaminor.fluentquery.intellij.model;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.util.PsiTreeUtil;
import dev.benjaminor.fluentquery.intellij.FluentQueryLightTestCase;

import java.util.List;

public class JpaEntityGraphAndPathResolverTest extends FluentQueryLightTestCase {

    public void testGraphFindsBasicsAndAssociations() {
        PsiClass user = myFixture.findClass("demo.User");
        JpaEntityGraph graph = JpaEntityGraphResolver.resolve(user);

        assertNotNull(graph.find("email"));
        assertEquals(JpaPropertyKind.BASIC, graph.find("email").kind());

        assertNotNull(graph.find("profile"));
        assertEquals(JpaPropertyKind.ASSOCIATION_TO_ONE, graph.find("profile").kind());
        assertEquals("demo.Profile", graph.find("profile").targetType().getQualifiedName());

        assertNotNull(graph.find("books"));
        assertEquals(JpaPropertyKind.ASSOCIATION_TO_MANY, graph.find("books").kind());
        assertEquals("demo.Book", graph.find("books").targetType().getQualifiedName());

        assertNull("scratch is @Transient", graph.find("scratch"));
        assertNull("serialVersionUID skipped", graph.find("serialVersionUID"));
    }

    public void testResolveAttributeAndNestedAssociation() {
        PsiClass user = myFixture.findClass("demo.User");

        PathResolveResult email = PathResolver.resolve(user, "email", false);
        assertTrue(email.isResolved());

        PathResolveResult nested = PathResolver.resolve(user, "profile.bio", false);
        assertTrue(nested.isResolved());
        assertEquals(2, nested.segments().size());

        PathResolveResult assoc = PathResolver.resolve(user, "profile", true);
        assertTrue(assoc.isResolved());
        assertEquals("demo.Profile", assoc.tipEntity().getQualifiedName());

        PathResolveResult bad = PathResolver.resolve(user, "nope", false);
        assertFalse(bad.isResolved());
        assertEquals("nope", bad.unresolvedSegment());

        PathResolveResult basicAsAssoc = PathResolver.resolve(user, "email", true);
        assertFalse(basicAsAssoc.isResolved());
    }

    public void testCompleteSuggestions() {
        PsiClass user = myFixture.findClass("demo.User");
        List<JpaProperty> root = PathResolver.complete(user, "em", false);
        assertTrue(root.stream().anyMatch(p -> "email".equals(p.name())));

        List<JpaProperty> nested = PathResolver.complete(user, "profile.b", false);
        assertTrue(nested.stream().anyMatch(p -> "bio".equals(p.name())));

        List<JpaProperty> assocs = PathResolver.complete(user, "", true);
        assertTrue(assocs.stream().anyMatch(p -> "profile".equals(p.name())));
        assertTrue(assocs.stream().noneMatch(p -> "email".equals(p.name())));
    }

    public void testCallAnalyzerResolvesRepositoryQuery() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                import dev.benjaminor.fluentquery.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().where("<caret>email", "a");
                  }
                }
                """);
        PsiLiteralExpression lit = PsiTreeUtil.getParentOfType(
                myFixture.getFile().findElementAt(myFixture.getCaretOffset()),
                PsiLiteralExpression.class);
        assertNotNull(lit);
        FluentQueryCallSite site = FluentQueryCallAnalyzer.analyze(lit);
        assertNotNull(site);
        assertEquals(FluentQueryPathRole.ATTRIBUTE, site.role());
        assertEquals("demo.User", site.entityType().getQualifiedName());
        assertEquals("email", site.pathText());
    }

    public void testPathValidatorFlagsUnknownAndDottedAttribute() {
        PsiClass user = myFixture.findClass("demo.User");
        List<PathValidator.Issue> unknown =
                PathValidator.validate(user, "missing", FluentQueryPathRole.ATTRIBUTE);
        assertEquals(1, unknown.size());

        List<PathValidator.Issue> dotted =
                PathValidator.validate(user, "profile.bio", FluentQueryPathRole.ATTRIBUTE);
        assertEquals(1, dotted.size());
        assertEquals("inspection.unresolved.path.dotted.attribute", dotted.get(0).messageKey());

        List<PathValidator.Issue> okFetch =
                PathValidator.validate(user, "profile", FluentQueryPathRole.ASSOCIATION);
        assertTrue(okFetch.isEmpty());
    }
}
