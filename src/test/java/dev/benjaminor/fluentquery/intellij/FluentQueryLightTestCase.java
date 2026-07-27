package dev.benjaminor.fluentquery.intellij;

import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

/**
 * Shared light-fixture setup: stub JPA + FluentQuery APIs and a small domain model.
 */
public abstract class FluentQueryLightTestCase extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected @NotNull LightProjectDescriptor getProjectDescriptor() {
        return JAVA_21;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        addJpaStubs();
        addFluentQueryStubs();
        addDomainModel();
    }

    protected void addJpaStubs() {
        myFixture.addClass("package jakarta.persistence;\npublic @interface Entity {}");
        myFixture.addClass("package jakarta.persistence;\npublic @interface ManyToOne {}");
        myFixture.addClass("package jakarta.persistence;\npublic @interface OneToOne {}");
        myFixture.addClass("package jakarta.persistence;\npublic @interface OneToMany {}");
        myFixture.addClass("package jakarta.persistence;\npublic @interface ManyToMany {}");
        myFixture.addClass("package jakarta.persistence;\npublic @interface Embedded {}");
        myFixture.addClass("package jakarta.persistence;\npublic @interface Embeddable {}");
        myFixture.addClass("package jakarta.persistence;\npublic @interface Transient {}");
    }

    protected void addFluentQueryStubs() {
        myFixture.addClass("""
                package dev.benjaminor.fluentquery;
                import java.util.function.Consumer;
                import java.util.Map;
                public final class FluentQuery<T> {
                  public FluentQuery<T> where(String column, Object value) { return this; }
                  public FluentQuery<T> whereEqual(String column, Object value) { return this; }
                  public FluentQuery<T> whereColumn(String left, String right) { return this; }
                  public FluentQuery<T> orderByAsc(String... columns) { return this; }
                  public FluentQuery<T> orderByDesc(String... columns) { return this; }
                  public FluentQuery<T> latest(String column) { return this; }
                  public FluentQuery<T> select(String... paths) { return this; }
                  public FluentQuery<T> fetch(String... associations) { return this; }
                  public FluentQuery<T> fetch(String association, Consumer<RelatedFilter> c) { return this; }
                  public FluentQuery<T> fetch(Map<String, Consumer<RelatedFilter>> relations) { return this; }
                  public FluentQuery<T> fetch(FetchRel... rels) { return this; }
                  public FluentQuery<T> whereHas(String relation) { return this; }
                  public FluentQuery<T> whereHas(String relation, Consumer<RelatedFilter> c) { return this; }
                  public FluentQuery<T> whereRelatedEqual(String relation, String column, Object value) { return this; }
                  public FluentQuery<T> whereRelation(String relation, String column, Object value) { return this; }
                  public static <T> FluentQuery<T> of(Object executor) { return new FluentQuery<>(); }
                }
                """);
        myFixture.addClass("""
                package dev.benjaminor.fluentquery;
                public final class RelatedFilter {
                  public RelatedFilter where(String column, Object value) { return this; }
                  public RelatedFilter whereLike(String column, String value) { return this; }
                }
                """);
        myFixture.addClass("""
                package dev.benjaminor.fluentquery;
                import java.util.function.Consumer;
                public final class FetchRel {
                  public FetchRel(String path, Consumer<RelatedFilter> constraints) {}
                  public static FetchRel of(String path) { return new FetchRel(path, null); }
                  public static FetchRel of(String path, Consumer<RelatedFilter> c) { return new FetchRel(path, c); }
                }
                """);
        myFixture.addClass("""
                package dev.benjaminor.fluentquery;
                public interface PropertyFilters<T> {
                  default Object hasPropertyEqual(String column, Object value) { return null; }
                  default Object hasRelatedPropertyEqual(String relation, String column, Object value) { return null; }
                  default Object hasRelation(String relation) { return null; }
                  default Object hasNoRelation(String relation) { return null; }
                }
                """);
        myFixture.addClass("""
                package dev.benjaminor.fluentquery;
                public interface FluentQueryRepository<T, ID> extends PropertyFilters<T> {
                  default FluentQuery<T> query() { return FluentQuery.of(this); }
                }
                """);
    }

    protected void addDomainModel() {
        myFixture.addClass("""
                package demo;
                import jakarta.persistence.*;
                import java.util.List;
                @Entity
                public class Profile {
                  public String bio;
                  public Boolean active;
                }
                """);
        myFixture.addClass("""
                package demo;
                import jakarta.persistence.*;
                @Entity
                public class Book {
                  public String title;
                  public Integer pages;
                }
                """);
        myFixture.addClass("""
                package demo;
                import jakarta.persistence.*;
                import java.util.List;
                @Entity
                public class User {
                  public Long id;
                  public String email;
                  public String name;
                  @ManyToOne public Profile profile;
                  @OneToMany public List<Book> books;
                  @Transient public String scratch;
                  public static final long serialVersionUID = 1L;
                }
                """);
        myFixture.addClass("""
                package demo;
                import jakarta.persistence.*;
                @Entity
                public class Account {
                  private Long id;
                  private String code;
                  private Profile profile;
                  public Long getId() { return id; }
                  public String getCode() { return code; }
                  @ManyToOne public Profile getProfile() { return profile; }
                }
                """);
        myFixture.addClass("""
                package demo;
                import dev.benjaminor.fluentquery.FluentQueryRepository;
                public interface BaseRepository<T, ID> extends FluentQueryRepository<T, ID> {}
                """);
        myFixture.addClass("""
                package demo;
                import dev.benjaminor.fluentquery.FluentQueryRepository;
                public interface UserRepository extends FluentQueryRepository<User, Long> {}
                """);
        myFixture.addClass("""
                package demo;
                public interface AccountRepository extends BaseRepository<Account, Long> {}
                """);
    }
}
