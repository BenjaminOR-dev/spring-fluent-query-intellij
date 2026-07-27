package dev.benjaminor.fluentquery.intellij.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FluentQueryMethodsTest {

    @Test
    public void attributeMethods() {
        assertEquals(
                FluentQueryPathRole.ATTRIBUTE,
                FluentQueryMethods.roleFor("where", FluentQueryMethods.FQ_FLUENT_QUERY, 0, 2));
        assertEquals(
                FluentQueryPathRole.ATTRIBUTE,
                FluentQueryMethods.roleFor("whereIf", FluentQueryMethods.FQ_FLUENT_QUERY, 1, 3));
        assertNull(FluentQueryMethods.roleFor("whereIf", FluentQueryMethods.FQ_FLUENT_QUERY, 0, 3));
    }

    @Test
    public void associationAndSelect() {
        assertEquals(
                FluentQueryPathRole.ASSOCIATION,
                FluentQueryMethods.roleFor("fetch", FluentQueryMethods.FQ_FLUENT_QUERY, 0, 1));
        assertEquals(
                FluentQueryPathRole.SELECT,
                FluentQueryMethods.roleFor("select", FluentQueryMethods.FQ_FLUENT_QUERY, 0, 1));
    }

    @Test
    public void relationThenAttribute() {
        assertEquals(
                FluentQueryPathRole.ASSOCIATION,
                FluentQueryMethods.roleFor(
                        "whereRelatedEqual", FluentQueryMethods.FQ_FLUENT_QUERY, 0, 3));
        assertEquals(
                FluentQueryPathRole.ATTRIBUTE,
                FluentQueryMethods.roleFor(
                        "whereRelatedEqual", FluentQueryMethods.FQ_FLUENT_QUERY, 1, 3));
        assertTrue(FluentQueryMethods.isRelationThenAttribute("whereRelation"));
    }

    @Test
    public void whereColumnPairAndTriple() {
        assertEquals(
                FluentQueryPathRole.ATTRIBUTE,
                FluentQueryMethods.roleFor("whereColumn", FluentQueryMethods.FQ_FLUENT_QUERY, 0, 2));
        assertEquals(
                FluentQueryPathRole.ATTRIBUTE,
                FluentQueryMethods.roleFor("whereColumn", FluentQueryMethods.FQ_FLUENT_QUERY, 1, 2));
        assertNull(FluentQueryMethods.roleFor("whereColumn", FluentQueryMethods.FQ_FLUENT_QUERY, 1, 3));
        assertEquals(
                FluentQueryPathRole.ATTRIBUTE,
                FluentQueryMethods.roleFor("whereColumn", FluentQueryMethods.FQ_FLUENT_QUERY, 2, 3));
    }

    @Test
    public void fetchRelOfOnlyWhenFetchRelClass() {
        assertEquals(
                FluentQueryPathRole.ASSOCIATION,
                FluentQueryMethods.roleFor("of", FluentQueryMethods.FQ_FETCH_REL, 0, 1));
        assertNull(FluentQueryMethods.roleFor("of", null, 0, 1));
        assertFalse(FluentQueryMethods.isKnownMethodName("of"));
    }

    @Test
    public void familyCheck() {
        assertTrue(FluentQueryMethods.isFluentQueryFamily(FluentQueryMethods.FQ_FLUENT_QUERY));
        assertTrue(FluentQueryMethods.isFluentQueryFamily(FluentQueryMethods.FQ_RELATED_FILTER));
        assertFalse(FluentQueryMethods.isFluentQueryFamily("java.util.Optional"));
    }
}
