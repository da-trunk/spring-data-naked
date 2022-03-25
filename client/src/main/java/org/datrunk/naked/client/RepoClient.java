package org.datrunk.naked.client;


import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.datrunk.naked.entities.CollectionDTO;
import org.datrunk.naked.entities.IdClass;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import uk.co.blackpepper.bowman.annotation.RemoteResource;

@Log4j2
public class RepoClient<T extends IdClass<ID>, ID> extends FunctionalClient {
    protected final RestTemplate restTemplate;
    private final Class<T> entityClass;
    private final Class<ID> idClass;
    private final String path;
    @Getter
    private final CEClient<T> client;
    private final CollectionDTO<T> queue = CollectionDTO.create();
    @Getter
    @Setter
    private int maxSize = 1;

    private final ParameterizedTypeReference<CollectionDTO<T>> typeRef;
    private final String naturalIdPath;
    private final String naturalIdName;
    private final Function<T, Object> naturalIdRetrievalFn;

    @ConfigurationProperties("client.repo")
    public static class Properties extends ClientProperties {};

    @Component
    public static class Factory {
        @Getter
        private final Properties properties;
        @Getter
        private final RestTemplate restTemplate;
        @Getter
        private final CEClient.Factory clientFactory;

        @Autowired
        protected Factory(Properties properties, RestTemplate restTemplate, CEClient.Factory clientFactory) {
            this.properties = properties;
            this.restTemplate = restTemplate;
            this.clientFactory = clientFactory;
        }

        public static String getPath(Class<?> entityClass) {
            if (entityClass.getAnnotation(RemoteResource.class) != null) {
                return entityClass.getAnnotation(RemoteResource.class)
                    .value();
            } else {
                return entityClass.getSimpleName()
                    .substring(0, 1)
                    .toLowerCase()
                    + entityClass.getSimpleName()
                        .substring(1)
                    + "s";
            }
        }

        public <T extends IdClass<ID>, ID> RepoClient<T, ID> create(Class<T> entityClass, Class<ID> idClass) {
            return create(entityClass, idClass, getPath(entityClass));
        }

        public <T extends IdClass<ID>, ID> RepoClient<T, ID> create(Class<T> entityClass, Class<ID> idClass, String path) {
            return new RepoClient<>(properties, path, restTemplate, clientFactory, entityClass, idClass, null, null, null);
        }

        public <T extends IdClass<ID>, ID> RepoClient<T, ID> create(Class<T> entityClass, Class<ID> idClass, String naturalIdPath,
            String naturalIdName, Function<T, Object> naturalIdRetrievalFn) {
            return new RepoClient<>(properties, getPath(entityClass), restTemplate, clientFactory, entityClass, idClass, naturalIdPath,
                naturalIdName, naturalIdRetrievalFn);
        }

        public <T extends IdClass<ID>, ID> RepoClient<T, ID> create(Class<T> entityClass, Class<ID> idClass, String path,
            String naturalIdPath, String naturalIdName, Function<T, Object> naturalIdRetrievalFn) {
            return new RepoClient<>(properties, path, restTemplate, clientFactory, entityClass, idClass, naturalIdPath, naturalIdName,
                naturalIdRetrievalFn);
        }

        public <T extends IdClass<ID>, ID> RepoClient<T, ID> create(Function<Factory, RepoClient<T, ID>> fn) {
            return fn.apply(this);
        }
    }

    public RepoClient(Properties properties, String path, RestTemplate restTemplate, CEClient.Factory clientFactory,
        Class<T> entityClass, Class<ID> idClass, String naturalIdPath, String naturalIdName, Function<T, Object> naturalIdRetrievalFn) {
        super(restTemplate, properties);
        this.restTemplate = restTemplate;
        this.entityClass = entityClass;
        this.idClass = idClass;
        this.typeRef = new ParameterizedTypeReference<CollectionDTO<T>>() {
            @Override
            public Type getType() {
                return new MyParameterizedTypeImpl((ParameterizedType) super.getType(), new Type[] { entityClass });
            }
        };
        this.path = path;
        client = clientFactory.create(entityClass);
        this.naturalIdPath = naturalIdPath;
        this.naturalIdName = naturalIdName;
        this.naturalIdRetrievalFn = naturalIdRetrievalFn;
    }

    @Override
    public UriComponentsBuilder getBaseURIBuilder() {
        return super.getBaseURIBuilder().pathSegment(path);
    }

    public Class<T> getContainedClass() {
        return entityClass;
    }

    public T get(ID id) {
        URI uri = getBaseURIBuilder().path(id.toString())
            .build()
            .toUri();
        return client.get(uri);
    }

    public List<T> getAll() {
        List<T> result = new ArrayList<>();
        client.getAll()
            .forEach(result::add);
        return result;
    }

    public List<T> findAll() {
        List<T> result = new ArrayList<>();
        client.findAll()
            .forEach(result::add);
        return result;
    }

    public T findByNaturalId(final T entity) {
        if (naturalIdName == null) {
            throw new IllegalStateException(String.format("Client<%s, %s> has not registered a natural id", entityClass, idClass));
        }
        return client.search(naturalIdPath, naturalIdName, naturalIdRetrievalFn.apply(entity));
    }

    @SuppressWarnings("unchecked")
    public T save(T entity) {
        URI uri;
        try {
            uri = client.post(entity);
        } catch (HttpClientErrorException.Conflict ex) {
            log.warn("RepoClient<{}, {}>::save was unable to persist {} due to \"{}\".  Assuming it has already been persisted.",
                entityClass.getSimpleName(), idClass.getSimpleName(), entity, ex.getMessage());
            final T existing;
            if (naturalIdName != null) {
                uri = entity.getUri();
                existing = findByNaturalId(entity);
            } else if (entity.getId() != null) {
                uri = entity.getUri();
                existing = client.get(uri);
            } else {
                throw ex;
            }
            if (!existing.equals(entity)) {
                assert (existing.getId() != null);
                assert (entity.getId() == null || entity.getId()
                    .equals(existing.getId()));
                assert (existing.getUri() != null);
                assert (entity.getUri() == null || entity.getUri()
                    .equals(existing.getUri()));
                final T patched = client.patch(existing.getUri(), entity);
                patched.setUri(existing.getUri());
                assert (patched != null);
                assert (patched.getId() != null);
                assert (patched.getUri() != null);
                assert (patched.equals(entity)); // if this is not true, we would need to assign entity = patched. That would create a copy
                // and force the client to update its references elsewhere.
            }
        }
        if (entity.getUri() == null) {
            entity.setUri(uri);
        } else {
            assert (entity.getUri()
                .equals(uri));
        }
        if (entity.getId() == null) {
            String[] parts = uri.getPath()
                .split("/");
            ID id = null;
            if (parts.length != 4) {
                // TODO: We may want to fall back to client.get(uri) in this case.
                log.warn(
                    "IMPORTANT: Expected 3 slashes in URI {} but I found {}.  Is this a compound identifier?  If so, that is not yet supported.");
            }
            try {
                // This follows spring-data-rest by taking only the last part of the URI. See UriToEntityConverter::convert. It won't work
                // with Entities that contain complex or composite keys. TODO: We should register converters with Spring for T <-> URI
                // conversion.
                String idStr = parts[parts.length - 1];
                for (final Constructor<?> constructor : idClass.getDeclaredConstructors()) {
                    if (constructor.getParameterCount() == 1) {
                        // This requires that ID can be constructed from a single String, Long, or Integer.
                        if (constructor.getParameterTypes()[0].equals(String.class)) {
                            id = (ID) BeanUtils.instantiateClass(constructor, idStr);
                        } else if (constructor.getParameterTypes()[0].equals(Long.class)) {
                            id = (ID) BeanUtils.instantiateClass(constructor, Long.valueOf(idStr));
                        } else if (constructor.getParameterTypes()[0].equals(Integer.class)) {
                            id = (ID) BeanUtils.instantiateClass(constructor, Integer.valueOf(idStr));
                        }
                    }
                }
            } catch (SecurityException e) {
                log.catching(e);
            }
            if (id != null) {
                entity.setId(id);
            } else {
                // We failed to parse this entity's id from the URI. We will try issuing a get request at this URI to retrieve the entity
                // and take the id from it.
                entity = client.get(uri);
                id = entity.getId();
                assert (id != null);
            }
        }
        return entity;
    }

    public void delete(T entity) {
        client.delete(entity.getUri());
    }

    public void deleteAll(Collection<T> entities) {
        for (T entity : entities)
            delete(entity);
    }

    public List<T> persist(T entity) {
        queue.add(entity);
        if (queue.size() >= maxSize) {
            return flush();
        }
        return Collections.emptyList();
    }

    public List<T> persist(Collection<T> entities) {
        List<T> flushResults = new ArrayList<>();
        for (T entity : entities) {
            List<T> flushed = persist(entity);
            flushResults.addAll(flushed);
        }
        return flushResults;
    }

    public Collection<T> persist(T first, @SuppressWarnings("unchecked") T... others) {
        return persist(Lists.asList(first, others));
    }

    public List<T> flush() {
        CollectionModel<EntityModel<T>> result = client.saveAll(queue);
        queue.clear();
        return result.getContent()
            .stream()
            .map(entityModel -> entityModel.getContent())
            .collect(Collectors.toList());
    }

    public List<T> saveAll(Collection<T> entities) {
        final int maxSize = getMaxSize();
        setMaxSize(entities.size());
        List<T> flushed = persist(entities);
        assert (flushed.size() == entities.size());
        setMaxSize(maxSize);
        return flushed;
    }

    public List<T> saveAllObsolete(List<T> entities) throws JsonProcessingException {
        try {
            final String json = new ObjectMapper().writeValueAsString(queue);
            log.info(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw e;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CollectionDTO<T>> request = new HttpEntity<>(queue, headers);
        URI uri = getBaseURIBuilder().build()
            .toUri();
        ResponseEntity<CollectionDTO<T>> actual = restTemplate.exchange(uri, HttpMethod.POST, request, typeRef);
        return actual.getBody()
            .getEntities();
    }
}
