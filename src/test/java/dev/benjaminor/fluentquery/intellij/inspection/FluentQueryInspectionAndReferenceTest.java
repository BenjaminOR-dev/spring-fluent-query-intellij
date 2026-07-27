package dev.benjaminor.fluentquery.intellij.inspection;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiReference;
import dev.benjaminor.fluentquery.intellij.FluentQueryLightTestCase;
import dev.benjaminor.fluentquery.intellij.reference.FluentQueryPathReference;

import java.util.List;

public class FluentQueryInspectionAndReferenceTest extends FluentQueryLightTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.enableInspections(FluentQueryUnresolvedPathInspection.class);
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
                i.getDescription() != null && i.getDescription().contains("cannot contain dots")));
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
}
