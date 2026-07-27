package dev.benjaminor.fluentquery.intellij.model;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SelectPathExpanderTest {

    @Test
    public void plainPathUnchanged() {
        assertEquals(List.of("email"), SelectPathExpander.expand("email"));
        assertEquals(List.of("profile.bio"), SelectPathExpander.expand("profile.bio"));
    }

    @Test
    public void shorthandExpandsColumns() {
        assertEquals(
                List.of("status.id", "status.name"),
                SelectPathExpander.expand("status:id,name"));
    }

    @Test
    public void nestedAssociationShorthand() {
        assertEquals(
                List.of("company.address.city", "company.address.zip"),
                SelectPathExpander.expand("company.address:city,zip"));
    }

    @Test
    public void blankAndMalformed() {
        assertTrue(SelectPathExpander.expand("").isEmpty());
        assertTrue(SelectPathExpander.expand("   ").isEmpty());
        assertEquals(List.of("status:"), SelectPathExpander.expand("status:"));
        assertEquals(List.of(":id"), SelectPathExpander.expand(":id"));
    }

    @Test
    public void trimsSegments() {
        assertEquals(
                List.of("status.id", "status.name"),
                SelectPathExpander.expand(" status : id , name "));
    }
}
