package dev.benjaminor.fluentquery.intellij.model;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SiblingPathArgsTest {

    @Test
    public void tracksVarargPathMethods() {
        assertTrue(SiblingPathArgs.tracksDuplicates("select"));
        assertTrue(SiblingPathArgs.tracksDuplicates("fetch"));
        assertTrue(SiblingPathArgs.tracksDuplicates("orderByAsc"));
        assertFalse(SiblingPathArgs.tracksDuplicates("where"));
        assertFalse(SiblingPathArgs.tracksDuplicates("whereColumn"));
    }

    @Test
    public void wouldDuplicateRootSuggestion() {
        Set<String> taken = Set.of("calle", "nombre");
        assertTrue(SiblingPathArgs.wouldDuplicate(
                taken, FluentQueryPathRole.SELECT, "", "calle"));
        assertFalse(SiblingPathArgs.wouldDuplicate(
                taken, FluentQueryPathRole.SELECT, "", "estatus"));
    }

    @Test
    public void wouldDuplicateNestedSuggestion() {
        Set<String> taken = Set.of("profile.bio");
        assertTrue(SiblingPathArgs.wouldDuplicate(
                taken, FluentQueryPathRole.SELECT, "profile.", "bio"));
        assertFalse(SiblingPathArgs.wouldDuplicate(
                taken, FluentQueryPathRole.SELECT, "profile.", "active"));
    }
}
