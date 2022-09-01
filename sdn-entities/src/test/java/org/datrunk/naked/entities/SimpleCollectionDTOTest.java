package org.datrunk.naked.entities;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.persistence.Transient;

import org.datrunk.naked.entities.config.EntityIdResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.lang.NonNull;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableList;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

@SpringBootTest(classes = SimpleCollectionDTOTest.Config.class, webEnvironment = WebEnvironment.NONE,
    properties = { "logging.level.org.datrunk.naked=debug" })
@ExtendWith({ SpringExtension.class })
@Log4j2
public class SimpleCollectionDTOTest {
    public static class Config {}

    @Target(TYPE)
    @Retention(RUNTIME)
    public @interface TestMarker {}

    protected interface Type {
        @Nonnull
        @JsonTypeId
        @Transient
        default public String getTypeId() {
            Class<?> type = getClass();
            while (type != Object.class && type.getAnnotation(TestMarker.class) == null) {
                type = type.getSuperclass();
            }
            if (type != Object.class)
                return type.getTypeName();
            else
                return getClass().getTypeName();
        }
    }

    @TestMarker
    @Getter
    @Setter
    @ToString(exclude = { "children" })
    @EqualsAndHashCode(callSuper = false, exclude = { "children" })
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Parent implements Type {
        @Nonnull
        private String id;

        @NonNull
        private List<Child> children;

        public Parent(String id, List<Child> children) {
            this.id = id;
            this.children = children;
        }

        public Parent(String id) {
            this.id = id;
            this.children = new ArrayList<>();
        }
    };

    @TestMarker
    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Child implements Type {
        @Nonnull
        private String id;

        @JsonIgnoreProperties("children")
        private Parent parent;

        public Child(String id, Parent parent) {
            assert (id != null);
            assert (parent != null);
            this.id = id;
            this.parent = parent;
            parent.getChildren()
                .add(this);
        }
    };

    @ToString(callSuper = true)
    public static class Boy extends Child {
        public Boy(String id, Parent parent) {
            super(id, parent);
        }
    };

    @ToString(callSuper = true)
    public static class Girl extends Child {
        public Girl(String id, Parent parent) {
            super(id, parent);
        }
    };

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class TestCollectionDTO<T extends Type> {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
        @JsonTypeIdResolver(value = EntityIdResolver.class)
        private List<T> entities = new ArrayList<>();

        @JsonCreator
        public TestCollectionDTO(@JsonProperty("entities") List<T> entities) {
            this.entities = entities;
        }
    }

    /**
     * Do not include handler when serializing a proxy object created by javassist.util.proxy to JSON.
     * 
     * @author Ansonator
     *
     */
    public static class ProxyModule extends SimpleModule {
        private static final long serialVersionUID = 1L;

        @JsonIgnoreType
        abstract static class MethodHandlerMixin {
            private MethodHandlerMixin() {}
        }

        public ProxyModule() {
            setMixInAnnotation(MethodHandler.class, MethodHandlerMixin.class);
        }
    }

    private ObjectMapper objectMapper;

    @BeforeEach
    public void before() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);
        assertThat(objectMapper).isNotNull();
    }

    @Test
    public void testProxySerialization() throws JsonProcessingException {
        Parent homer = new Parent("Homer");

        final Child proxy;
        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(Child.class);
        Class<?> clazz = factory.createClass();
        try {
            proxy = (Child) clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
        proxy.setId("Lisa");
        proxy.setParent(homer);
        homer.getChildren()
            .add(proxy);

        TestCollectionDTO<Child> dto = new TestCollectionDTO<>(homer.getChildren());

        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.registerModule(new ProxyModule());
        String json = objectMapper.writeValueAsString(dto);
        log.debug("json = {}]", json);
        TestCollectionDTO<?> actual = objectMapper.readValue(json, TestCollectionDTO.class);
        log.debug("actual = {}", actual);
    }

    @Test
    public void testTypeId() throws JsonProcessingException {
        Parent homer = new Parent("Homer");
        Boy bart = new Boy("Bart", homer);
        Girl lisa = new Girl("Lisa", homer);
        List<Child> children = ImmutableList.of(bart, lisa);
        homer.setChildren(children);
        TestCollectionDTO<Child> dto = new TestCollectionDTO<>(children);
        assertThat(objectMapper).isNotNull();
        String json = objectMapper.writeValueAsString(dto);
        log.debug("json = {}", json);

        TestCollectionDTO<?> actual = objectMapper.readValue(json, TestCollectionDTO.class);
        log.debug("actual = {}", actual);
        Parent expectedParent = new Parent(homer.getId());
        List<Child> expectedChildren = Stream.of(bart, lisa)
            .map(c -> new Child(c.getId(), expectedParent))
            .collect(Collectors.toList());
        TestCollectionDTO<Child> expected = new TestCollectionDTO<>(expectedChildren);
        assertThat(actual).isEqualTo(expected);
    }

}
