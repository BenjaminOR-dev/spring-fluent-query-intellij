package dev.benjaminor.fluentquery.intellij.completion;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FluentQueryPathCompletionContributorTest {

    @Test
    public void segmentFragmentAfterDot() {
        assertEquals("", FluentQueryPathCompletionContributor.segmentFragment("profile."));
        assertEquals("bi", FluentQueryPathCompletionContributor.segmentFragment("profile.bi"));
        assertEquals("email", FluentQueryPathCompletionContributor.segmentFragment("email"));
        assertEquals("", FluentQueryPathCompletionContributor.segmentFragment("a.b."));
    }
}
