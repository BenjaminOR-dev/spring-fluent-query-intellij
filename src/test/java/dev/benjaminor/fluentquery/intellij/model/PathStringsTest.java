package dev.benjaminor.fluentquery.intellij.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PathStringsTest {

    @Test
    public void stripsDummyIdentifiers() {
        assertEquals(
                "email",
                PathStrings.stripCompletionDummy("emailIntellijIdeaRulezzz"));
        assertEquals(
                "profile.",
                PathStrings.stripCompletionDummy("profile.IntellijIdeaRulezzz"));
    }
}
