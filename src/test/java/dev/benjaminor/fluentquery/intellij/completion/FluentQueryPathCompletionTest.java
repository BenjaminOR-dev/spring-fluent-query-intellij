package dev.benjaminor.fluentquery.intellij.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import dev.benjaminor.fluentquery.intellij.FluentQueryLightTestCase;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class FluentQueryPathCompletionTest extends FluentQueryLightTestCase {

    public void testCompletesRootAttributes() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().where("<caret>", null);
                  }
                }
                """);
        myFixture.completeBasic();
        Set<String> names = lookupNames();
        assertTrue(names.contains("email"));
        assertTrue(names.contains("name"));
        assertTrue(names.contains("profile"));
    }

    public void testCompletesNestedAssociationAttributes() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().select("profile.<caret>");
                  }
                }
                """);
        myFixture.completeBasic();
        Set<String> names = lookupNames();
        assertTrue(names.contains("bio"));
        assertTrue(names.contains("active"));
    }

    public void testCompletesAssociationsForFetch() {
        myFixture.configureByText("Use.java", """
                import demo.*;
                class Use {
                  void run(UserRepository repo) {
                    repo.query().fetch("<caret>");
                  }
                }
                """);
        myFixture.completeBasic();
        Set<String> names = lookupNames();
        assertTrue(names.contains("profile"));
        assertTrue(names.contains("books"));
        assertFalse(names.contains("email"));
    }

    private Set<String> lookupNames() {
        LookupElement[] elements = myFixture.getLookupElements();
        assertNotNull("expected completion variants", elements);
        return Arrays.stream(elements)
                .map(LookupElement::getLookupString)
                .collect(Collectors.toSet());
    }
}
