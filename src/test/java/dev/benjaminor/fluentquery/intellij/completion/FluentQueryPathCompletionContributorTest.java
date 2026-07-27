package dev.benjaminor.fluentquery.intellij.completion;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FluentQueryPathCompletionContributorTest {

    @Test
    public void segmentFragmentAfterDot() {
        assertEquals("", FluentQueryPathCompletionContributor.segmentFragment("datosKyc."));
        assertEquals("uso", FluentQueryPathCompletionContributor.segmentFragment("datosKyc.uso"));
        assertEquals("estatus", FluentQueryPathCompletionContributor.segmentFragment("estatus"));
        assertEquals("", FluentQueryPathCompletionContributor.segmentFragment("a.b."));
    }
}
