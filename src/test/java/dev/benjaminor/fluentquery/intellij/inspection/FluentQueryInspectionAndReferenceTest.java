package dev.benjaminor.fluentquery.intellij.inspection;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiReference;
import dev.benjaminor.fluentquery.intellij.FluentQueryLightTestCase;
import dev.benjaminor.fluentquery.intellij.reference.FluentQueryPathReference;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FluentQueryInspectionAndReferenceTest extends FluentQueryLightTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.enableInspections(
                FluentQueryUnresolvedPathInspection.class,
                FluentQueryFetchWithProjectionInspection.class,
                FluentQueryFetchCollectionWithPaginationInspection.class,
                FluentQueryDeprecatedAsInspection.class,
                FluentQuerySelectWithoutProjectionInspection.class);
    }

    public void testInspectionFlagsFetchWithFirstClass() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().fetch("profile").select("id", "email").first(Object.class);
                  }
                }
                """);
        List<HighlightInfo> infos = myFixture.doHighlighting(HighlightSeverity.ERROR);
        assertTrue(infos.stream().anyMatch(i ->
                i.getDescription() != null
                        && i.getDescription().contains("fetch()")
                        && i.getDescription().contains("first")));
    }

    public void testInspectionSilentOnEntityFirstWithFetch() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().fetch("profile").first();
                  }
                }
                """);
        List<HighlightInfo> infos = myFixture.doHighlighting(HighlightSeverity.ERROR);
        assertTrue(infos.stream().noneMatch(i ->
                i.getDescription() != null && i.getDescription().contains("cannot be combined")));
    }

    public void testInspectionSilentOnSelectFirstClassWithoutFetch() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().select("id", "email").first(Object.class);
                  }
                }
                """);
        List<HighlightInfo> infos = myFixture.doHighlighting(HighlightSeverity.ERROR);
        assertTrue(infos.stream().noneMatch(i ->
                i.getDescription() != null && i.getDescription().contains("cannot be combined")));
    }

    public void testFetchCollectionWithPageIsError() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().fetchCollection("books").page(null);
                  }
                }
                """);
        List<HighlightInfo> infos = myFixture.doHighlighting(HighlightSeverity.ERROR);
        assertTrue(infos.stream().anyMatch(i ->
                i.getDescription() != null
                        && i.getDescription().contains("fetchCollection")
                        && i.getDescription().contains("page")));
    }

    public void testDeprecatedAsIsWarning() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().select("id", "email").firstAs(Object.class);
                  }
                }
                """);
        List<HighlightInfo> infos = myFixture.doHighlighting(HighlightSeverity.WARNING);
        assertTrue(infos.stream().anyMatch(i ->
                i.getDescription() != null
                        && i.getDescription().contains("firstAs")
                        && i.getDescription().contains("deprecated")));
    }

    public void testSelectWithoutProjectionIsWeakWarning() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().select("id", "email").first();
                  }
                }
                """);
        List<HighlightInfo> infos = myFixture.doHighlighting(HighlightSeverity.WEAK_WARNING);
        assertTrue(infos.stream().anyMatch(i ->
                i.getDescription() != null
                        && i.getDescription().contains("select")
                        && i.getDescription().contains("partial entity")));
    }

    public void testStaticFinalStringConstantPathIsValidated() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  static final String BAD = "noSuchField";
                  void run(UserRepository repo) {
                    repo.query().where(BAD, "x");
                  }
                }
                """);
        List<HighlightInfo> infos = myFixture.doHighlighting(HighlightSeverity.ERROR);
        assertTrue(infos.stream().anyMatch(i ->
                i.getDescription() != null && i.getDescription().contains("noSuchField")));
    }

    public void testStaticFinalStringConstantValidPathSilent() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  static final String EMAIL = "email";
                  void run(UserRepository repo) {
                    repo.query().where(EMAIL, "x");
                  }
                }
                """);
        List<HighlightInfo> infos = myFixture.doHighlighting(HighlightSeverity.ERROR);
        assertTrue(infos.stream().noneMatch(i ->
                i.getDescription() != null && i.getDescription().contains("email")));
    }

    public void testInspectionHighlightsUnknownWherePath() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().where("noSuchField", "x");
                  }
                }
                """);
        List<HighlightInfo> infos = myFixture.doHighlighting(HighlightSeverity.ERROR);
        assertFalse(infos.isEmpty());
        assertTrue(infos.stream().anyMatch(i ->
                i.getDescription() != null && i.getDescription().contains("noSuchField")));
    }

    public void testInspectionSilentOnValidPath() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().where("email", "x").fetch("profile");
                  }
                }
                """);
        List<HighlightInfo> infos = myFixture.doHighlighting(HighlightSeverity.ERROR);
        assertTrue(infos.stream().noneMatch(i ->
                i.getDescription() != null && i.getDescription().contains("FluentQuery")));
    }

    public void testInspectionFlagsDottedAttribute() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().where("profile.bio", "x");
                  }
                }
                """);
        List<HighlightInfo> infos = myFixture.doHighlighting(HighlightSeverity.ERROR);
        assertTrue(infos.stream().anyMatch(i ->
                i.getDescription() != null && i.getDescription().contains("contains dots")));
    }

    public void testInspectionFlagsColonInFetch() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().fetch("profile:bio");
                  }
                }
                """);
        List<HighlightInfo> infos = myFixture.doHighlighting(HighlightSeverity.ERROR);
        assertTrue(infos.stream().anyMatch(i ->
                i.getDescription() != null && i.getDescription().contains("':'")));
    }

    public void testOrderByAllowsNestedProperty() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().orderByAsc("profile.bio");
                  }
                }
                """);
        List<HighlightInfo> infos = myFixture.doHighlighting(HighlightSeverity.ERROR);
        assertTrue(infos.stream().noneMatch(i ->
                i.getDescription() != null && i.getDescription().contains("profile")));
    }

    public void testMapOfFetchKey() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                import java.util.Map;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().fetch(Map.of("<caret>profile", f -> {}));
                  }
                }
                """);
        PsiReference ref = myFixture.getReferenceAtCaretPositionWithAssertion();
        assertNotNull(ref.resolve());
    }

    public void testSelectShorthandColumnReference() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().select("profile:<caret>bio");
                  }
                }
                """);
        PsiReference ref = myFixture.getReferenceAtCaretPositionWithAssertion();
        assertNotNull(ref.resolve());
        assertEquals("bio", ((com.intellij.psi.PsiNamedElement) ref.resolve()).getName());
    }

    public void testPropertyFiltersHasProperty() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.hasPropertyEqual("<caret>email", "x");
                  }
                }
                """);
        PsiReference ref = myFixture.getReferenceAtCaretPositionWithAssertion();
        assertNotNull(ref.resolve());
    }

    public void testPathReferenceResolvesToField() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().where("<caret>email", "x");
                  }
                }
                """);
        PsiReference ref = myFixture.getReferenceAtCaretPositionWithAssertion();
        assertInstanceOf(ref, FluentQueryPathReference.class);
        assertNotNull(ref.resolve());
        assertInstanceOf(ref.resolve(), com.intellij.psi.PsiNamedElement.class);
        assertEquals("email", ((com.intellij.psi.PsiNamedElement) ref.resolve()).getName());
    }

    public void testFetchAssociationReference() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().fetch("<caret>profile");
                  }
                }
                """);
        PsiReference ref = myFixture.getReferenceAtCaretPositionWithAssertion();
        assertNotNull(ref.resolve());
    }

    public void testDuplicateSelectPathIsError() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().select("email", "email");
                  }
                }
                """);
        List<HighlightInfo> infos = myFixture.doHighlighting(HighlightSeverity.ERROR);
        assertTrue(infos.stream().anyMatch(i ->
                i.getDescription() != null && i.getDescription().contains("Duplicate path")));
    }

    public void testCompletesSelectExcludingUsed() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().select("email", "name", "<caret>");
                  }
                }
                """);
        myFixture.completeBasic();
        LookupElement[] elements = myFixture.getLookupElements();
        assertNotNull(elements);
        Set<String> names = Arrays.stream(elements)
                .map(LookupElement::getLookupString)
                .collect(Collectors.toSet());
        assertFalse(names.contains("email"));
        assertFalse(names.contains("name"));
        assertTrue(names.contains("id"));
    }

    public void testRelatedFilterLeafColumn() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().whereHas("books", f -> f.where("<caret>pages", 10));
                  }
                }
                """);
        PsiReference ref = myFixture.getReferenceAtCaretPositionWithAssertion();
        assertNotNull("should resolve Book.pages", ref.resolve());
        assertInstanceOf(ref.resolve(), com.intellij.psi.PsiNamedElement.class);
        assertEquals("pages", ((com.intellij.psi.PsiNamedElement) ref.resolve()).getName());
    }

    public void testBaseRepositoryGenericHierarchy() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(AccountRepository repo) {
                    repo.query().where("<caret>code", "x");
                  }
                }
                """);
        PsiReference ref = myFixture.getReferenceAtCaretPositionWithAssertion();
        assertNotNull("should resolve Account.code via BaseRepository<Account>", ref.resolve());
    }

    public void testPropertyAccessGetterAssociation() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(AccountRepository repo) {
                    repo.query().fetch("<caret>profile");
                  }
                }
                """);
        PsiReference ref = myFixture.getReferenceAtCaretPositionWithAssertion();
        assertNotNull("should resolve getProfile() association", ref.resolve());
    }

    public void testMapOfEntriesKey() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                import java.util.Map;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().fetch(Map.ofEntries(Map.entry("<caret>profile", f -> {})));
                  }
                }
                """);
        PsiReference ref = myFixture.getReferenceAtCaretPositionWithAssertion();
        assertNotNull(ref.resolve());
    }
}
