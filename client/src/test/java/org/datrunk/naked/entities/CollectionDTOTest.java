package org.datrunk.naked.entities;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.databind.JavaType;

import org.datrunk.naked.entities.bowman.annotation.LinkedResource;
import org.datrunk.naked.entities.bowman.annotation.RemoteResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import javassist.util.proxy.ProxyFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import uk.co.blackpepper.bowman.ClientProxyException;
import uk.co.blackpepper.bowman.ClientProxyFactory;
import uk.co.blackpepper.bowman.Configuration;
import uk.co.blackpepper.bowman.JacksonClientModule;
import uk.co.blackpepper.bowman.JavassistClientProxyFactory;
import uk.co.blackpepper.bowman.RestOperations;
import uk.co.blackpepper.bowman.TypeResolver;
import uk.co.blackpepper.bowman.annotation.ResourceTypeInfo;

@SpringBootTest(classes = CollectionDTOTest.Config.class, webEnvironment = WebEnvironment.NONE,
    properties = { "spring.main.lazy-initialization=true", "logging.level.com.cerner.concept.mapping.support.jpa.repo=debug" })
@ExtendWith({ SpringExtension.class })
@Log4j2
public class CollectionDTOTest {

    public static class Config {
        @Bean
        public ObjectMapper getObjectMapper() {
            return new ObjectMapper();
        }
    }

    @Entity
    @Getter
    @Setter
    @ToString(exclude = { "children" })
    @RemoteResource("/parents")
    public static class Parent extends IdClass<String> {
        @Id
        @NonNull
        private String id;

        @Override
        public void setId(String id) {
            this.id = id;
        }

        @NonNull
        @OneToMany(fetch = FetchType.LAZY, mappedBy = "parent", orphanRemoval = true)
        @Getter(onMethod_ = { @LinkedResource })
        private Set<Child> children = new HashSet<>();

        protected Parent() {
            super(); // Parent.class);
        }

        public Parent(String id) {
            this();
            this.id = id;
        }

        public static Parent of(String id) {
            return new Parent(id);
        }
    };

    @Entity
    @Getter
    @Setter
    @ToString
    @RemoteResource("/children")
    public static class Child extends IdClass<String> {
        @Id
        @NonNull
        private String id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JsonIgnoreProperties("children")
        @Getter(onMethod_ = { @LinkedResource })
        private Parent parent;

        protected Child() {
            super(); // Child.class);
        }

        public Child(String id) {
            this();
            this.id = id;
        }

        public static Child of(String id) {
            return new Child(id);
        }
    };

    @Test
    public void testDeserializationWithTypeRef() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<CollectionDTO<Parent>> typeRef = new TypeReference<CollectionDTO<Parent>>() {};

        List<Parent> parents = ImmutableList.of(Parent.of("Homer"), Parent.of("Marge"));
        CollectionDTO<Parent> dto = CollectionDTO.create(parents);

        final String json = objectMapper.writeValueAsString(dto);
        log.debug("json = [{}]", json);
        CollectionDTO<Parent> actual = objectMapper.readValue(json, typeRef);
        log.debug("actual = [{}]", actual);
    }

    public static <T extends Annotation> T getAnnotation(Class<?> clazz, Class<T> annotationType) {
        T result = clazz.getAnnotation(annotationType);
        if (result == null) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null) {
                result = getAnnotation(superclass, annotationType);
            }
        }
        if (result == null) {
            for (Class<?> inf : clazz.getInterfaces()) {
                result = getAnnotation(inf, annotationType);
                if (result != null)
                    break;
            }
        }
        return result;
    }

    @Test
    // https://stackoverflow.com/questions/12353774/how-to-customize-jackson-type-information-mechanism
    public void testDeserializationWithoutTypeRef() throws JsonMappingException, JsonProcessingException {
        String baseUri = "http://bogus";
        final RestOperationsInstantiation restOperationsFactory = new RestOperationsInstantiation("http://bogus");
        ObjectMapper objectMapper = restOperationsFactory.getObjectMapper();

        String parentPath = Parent.class.getAnnotation(RemoteResource.class)
            .value();
        URI parentBaseUri = UriComponentsBuilder.fromUri(URI.create(baseUri))
            .path(parentPath)
            .build()
            .toUri();

        Parent homer = Parent.of("Homer");
        RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(mockRestTemplate.postForLocation(parentBaseUri, homer))
            .thenReturn(UriComponentsBuilder.fromUri(parentBaseUri)
                .path("/homer")
                .build()
                .toUri());
        {
            URI uri = mockRestTemplate.postForLocation(parentBaseUri, homer);
            homer.setUri(uri);
        }
        final CollectionDTO<?> actual;
        {
            Child bart = Child.of("Bart");
            final Child proxy;
            ProxyFactory factory = new ProxyFactory();
            factory.setSuperclass(Child.class);
            Class<?> clazz = factory.createClass();
            try {
                proxy = (Child) clazz.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(e);
            }
            proxy.setId("Baby");

            Set<Child> children = ImmutableSet.of(proxy, bart);
            homer.setChildren(children);
            children.stream()
                .forEach(child -> {
                    child.setParent(homer);
                });
            CollectionDTO<Child> dto = CollectionDTO.create(children);

            log.debug("entities = {}", children);
            String json = objectMapper.writeValueAsString(dto);
            log.debug("json = {}", json);
            objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, false);
            actual = objectMapper.readValue(json, CollectionDTO.class);
            log.debug("actual = {}", actual);
        }
    }

    private static class RestOperationsInstantiation extends HandlerInstantiator {
        @Getter
        private final RestOperations restOperations;
        private final Map<Class<?>, Object> handlerMap = new HashMap<>();
        @Getter
        private final ObjectMapper objectMapper;
        @Getter
        private final RestTemplate restTemplate;
        private final ClientProxyFactory proxyFactory;
        private final uk.co.blackpepper.bowman.Configuration configuration;

        static class JsonClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
                HttpRequestWrapper wrapped = new HttpRequestWrapper(request);
                wrapped.getHeaders()
                    .put("Content-Type", asList(MediaTypes.HAL_JSON_VALUE));
                wrapped.getHeaders()
                    .put("Accept", asList(MediaTypes.HAL_JSON_VALUE));
                return execution.execute(wrapped, body);
            }
        }

        public RestOperationsInstantiation(String baseUrl) {
            configuration = uk.co.blackpepper.bowman.Configuration.builder()
                .setBaseUri(baseUrl)
                .build();

            proxyFactory = new JavassistClientProxyFactory();

            objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.registerModule(new Jackson2HalModule());
            objectMapper.registerModule(new JacksonClientModule());
            objectMapper.setHandlerInstantiator(this);

            restTemplate = new RestTemplate(configuration.getClientHttpRequestFactory());
            restTemplate.getMessageConverters()
                .add(0, new MappingJackson2HttpMessageConverter(objectMapper));
            restTemplate.getInterceptors()
                .add(new JsonClientHttpRequestInterceptor());

            if (configuration.getRestTemplateConfigurer() != null) {
                configuration.getRestTemplateConfigurer()
                    .configure(restTemplate);
            }

            if (configuration.getObjectMapperConfigurer() != null) {
                configuration.getObjectMapperConfigurer()
                    .configure(objectMapper);
            }

            restOperations = new RestOperations(restTemplate, objectMapper);

            handlerMap.put(ResourceDeserializer.class, new ResourceDeserializer(Object.class, new DefaultTypeResolver(), configuration));

            handlerMap.put(InlineAssociationDeserializer.class,
                new InlineAssociationDeserializer<>(Object.class, restOperations, proxyFactory));
        }

        @Override
        public JsonDeserializer<?> deserializerInstance(DeserializationConfig config, Annotated annotated, Class<?> deserClass) {
            return (JsonDeserializer<?>) findHandlerInstance(deserClass);
        }

        @Override
        public KeyDeserializer keyDeserializerInstance(DeserializationConfig config, Annotated annotated, Class<?> keyDeserClass) {
            return (KeyDeserializer) findHandlerInstance(keyDeserClass);
        }

        @Override
        public JsonSerializer<?> serializerInstance(SerializationConfig config, Annotated annotated, Class<?> serClass) {
            return (JsonSerializer<?>) findHandlerInstance(serClass);
        }

        @Override
        public TypeResolverBuilder<?> typeResolverBuilderInstance(MapperConfig<?> config, Annotated annotated, Class<?> builderClass) {
            return (TypeResolverBuilder<?>) findHandlerInstance(builderClass);
        }

        @Override
        public TypeIdResolver typeIdResolverInstance(MapperConfig<?> config, Annotated annotated, Class<?> resolverClass) {
            return (TypeIdResolver) findHandlerInstance(resolverClass);
        }

        private Object findHandlerInstance(Class<?> clazz) {
            Object handler = handlerMap.get(clazz);
            return handler != null ? handler : BeanUtils.instantiateClass(clazz);
        }

        static class InlineAssociationDeserializer<T> extends StdDeserializer<T> implements ContextualDeserializer {

            private static final long serialVersionUID = -8694505834979017488L;

            private Class<T> type;

            private RestOperations restOperations;

            private ClientProxyFactory proxyFactory;

            InlineAssociationDeserializer(Class<T> type, RestOperations restOperations, ClientProxyFactory proxyFactory) {
                super(type);
                this.type = type;
                this.restOperations = restOperations;
                this.proxyFactory = proxyFactory;
            }

            @Override
            public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                JavaType resourceType = ctxt.getTypeFactory()
                    .constructParametricType(EntityModel.class, type);

                EntityModel<T> resource = p.getCodec()
                    .readValue(p, resourceType);

                return proxyFactory.create(resource, restOperations);
            }

            @Override
            public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
                return new InlineAssociationDeserializer<>(ctxt.getContextualType()
                    .getRawClass(), restOperations, proxyFactory);
            }
        }

        static class ResourceDeserializer extends StdDeserializer<EntityModel<?>> implements ContextualDeserializer {

            private static final long serialVersionUID = -7290132544264448620L;

            private TypeResolver typeResolver;

            private Configuration configuration;

            ResourceDeserializer(Class<?> type, TypeResolver typeResolver, Configuration configuration) {
                super(type);
                this.typeResolver = typeResolver;
                this.configuration = configuration;
            }

            @Override
            public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
                Class<?> resourceContentType = ctxt.getContextualType()
                    .getBindings()
                    .getTypeParameters()
                    .get(0)
                    .getRawClass();

                return new ResourceDeserializer(resourceContentType, typeResolver, configuration);
            }

            @Override
            public EntityModel<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                ObjectNode node = p.readValueAs(ObjectNode.class);

                ObjectMapper mapper = (ObjectMapper) p.getCodec();

                RepresentationModel resource = mapper.convertValue(node, RepresentationModel.class);
                Links links = Links.of(resource.getLinks());

                Object content = mapper.convertValue(node, getResourceDeserializationType(links));
                return EntityModel.of(content, links);
            }

            private Class<?> getResourceDeserializationType(Links links) {
                Class<?> resourceContentType = typeResolver.resolveType(handledType(), links, configuration);

                if (resourceContentType.isInterface()) {
                    ProxyFactory factory = new ProxyFactory();
                    factory.setInterfaces(new Class[] { resourceContentType });
                    resourceContentType = factory.createClass();
                } else if (Modifier.isAbstract(resourceContentType.getModifiers())) {
                    ProxyFactory factory = new ProxyFactory();
                    factory.setSuperclass(resourceContentType);
                    resourceContentType = factory.createClass();
                }

                return resourceContentType;
            }
        }
    }

    static class DefaultTypeResolver implements TypeResolver {
        @Override
        public <T> Class<? extends T> resolveType(Class<T> declaredType, Links resourceLinks, Configuration config) {
            ResourceTypeInfo info = AnnotationUtils.findAnnotation(declaredType, ResourceTypeInfo.class);
            if (info == null) {
                return declaredType;
            }
            boolean customTypeResolverIsSpecified = info.typeResolver() != ResourceTypeInfo.NullTypeResolver.class;
            if (!(info.subtypes().length > 0 ^ customTypeResolverIsSpecified)) {
                throw new ClientProxyException("one of subtypes or typeResolver must be specified");
            }
            TypeResolver delegateTypeResolver =
                customTypeResolverIsSpecified ? BeanUtils.instantiateClass(info.typeResolver()) : new SelfLinkTypeResolver(info.subtypes());
            return delegateTypeResolver.resolveType(declaredType, resourceLinks, config);
        }
    }

    static class SelfLinkTypeResolver implements TypeResolver {

        private Class<?>[] subtypes;

        SelfLinkTypeResolver(Class<?>[] subtypes) {
            this.subtypes = subtypes;
        }

        @Override
        public <T> Class<? extends T> resolveType(Class<T> declaredType, Links resourceLinks, Configuration configuration) {

            Optional<Link> self = resourceLinks.getLink(IanaLinkRelations.SELF);

            if (!self.isPresent()) {
                return declaredType;
            }

            for (Class<?> candidateClass : subtypes) {
                RemoteResource candidateClassInfo = AnnotationUtils.findAnnotation(candidateClass, RemoteResource.class);

                if (candidateClassInfo == null) {
                    throw new ClientProxyException(
                        String.format("%s is not annotated with @%s", candidateClass.getName(), RemoteResource.class.getSimpleName()));
                }

                String resourcePath = candidateClassInfo.value();

                String resourceBaseUriString = UriComponentsBuilder.fromUri(configuration.getBaseUri())
                    .path(resourcePath)
                    .toUriString();

                String selfLinkUriString = toAbsoluteUriString(self.get()
                    .getHref(), configuration.getBaseUri());

                if (selfLinkUriString.startsWith(resourceBaseUriString + "/")) {
                    if (!declaredType.isAssignableFrom(candidateClass)) {
                        throw new ClientProxyException(
                            String.format("%s is not a subtype of %s", candidateClass.getName(), declaredType.getName()));
                    }

                    @SuppressWarnings("unchecked")
                    Class<? extends T> result = (Class<? extends T>) candidateClass;

                    return result;
                }
            }

            return declaredType;
        }

        private static String toAbsoluteUriString(String uri, URI baseUri) {
            if (UriComponentsBuilder.fromUriString(uri)
                .build()
                .getHost() != null) {
                return uri;
            }

            return UriComponentsBuilder.fromUri(baseUri)
                .path(uri)
                .toUriString();
        }
    }
}
