/*
 * Copyright 2016 Black Pepper Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.datrunk.naked.client;

import java.net.URI;
import java.util.Collections;

import org.datrunk.naked.entities.CollectionDTO;
import org.datrunk.naked.entities.WithId;
import org.datrunk.naked.entities.WithUri;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.log4j.Log4j2;
import uk.co.blackpepper.bowman.Client;
import uk.co.blackpepper.bowman.ClientProxyFactory;
import uk.co.blackpepper.bowman.Configuration;
import uk.co.blackpepper.bowman.DefaultObjectMapperFactory;
import uk.co.blackpepper.bowman.DefaultRestTemplateFactory;
import uk.co.blackpepper.bowman.JavassistClientProxyFactory;
import uk.co.blackpepper.bowman.ObjectMapperFactory;
import uk.co.blackpepper.bowman.RestOperations;
import uk.co.blackpepper.bowman.RestOperationsFactory;
import uk.co.blackpepper.bowman.RestTemplateFactory;

@Log4j2
public class CEClient<T extends WithUri & WithId> extends Client<T> {
    private final Configuration bowmanConfiguration;
    protected final URI batchUri;

    protected CEClient(Class<T> entityType, String batchPath, Configuration bowmanConfiguration, RestOperations restOperations,
        ClientProxyFactory proxyFactory) {
        this(entityType, UriComponentsBuilder.fromUri(bowmanConfiguration.getBaseUri())
            .path(batchPath)
            .build()
            .toUri(), bowmanConfiguration, restOperations, proxyFactory);
    }

    protected CEClient(Class<T> entityType, URI batchUri, Configuration bowmanConfiguration, RestOperations restOperations,
        ClientProxyFactory proxyFactory) {
        super(entityType, bowmanConfiguration, restOperations, proxyFactory);
        this.bowmanConfiguration = bowmanConfiguration;
        this.batchUri = batchUri;
        log.debug("CEClient<{}>: baseUri = [{}], batchUri = [{}]", entityType, getBaseUri(), batchUri);
    }

    protected Configuration getConfiguration() {
        return bowmanConfiguration;
    }

    /**
     * Create a new <code>CEClient</code> for the given annotated entity type.
     * 
     * @param <O> the entity type of the required client
     * @param entityType the entity type of the required client
     * @return the created client
     */
    public <O extends WithUri & WithId> CEClient<O> create(Class<O> entityType) {
        return new CEClient<>(entityType, batchUri, getConfiguration(), getRestOperations(), getProxyFactory());
    }

    @Override
    public T get(URI uri) {
        T result = super.get(uri);
        if (result != null)
            result.setUri(uri);
        return result;
    }

    // http://localhost:9080/api/remoteResource/search/findAll
    public Iterable<T> findAll() {
        URI uri = UriComponentsBuilder.fromUri(getBaseUri())
            .pathSegment("search", "findAll")
            .build()
            .toUri();
        Iterable<T> entities = getAll(uri);
        return entities;
    }

    // http://localhost:9080/api/remoteResource/search/path?name=val
    public T search(String path, String name, Object val) {
        URI uri = UriComponentsBuilder.fromUri(getBaseUri())
            .pathSegment("search", path)
            .queryParam(name, val)
            .build()
            .toUri();
        return get(uri);
    }

    @Override
    public void put(T entity) {
        getEntityUri(entity);
        super.put(entity);
    }

    protected URI getEntityUri(T entity) {
        URI uri = UriComponentsBuilder.fromUri(getBaseUri())
            .pathSegment(entity.getId()
                .toString())
            .build()
            .toUri();
        if (entity.getUri() == null)
            entity.setUri(uri);
        return uri;
    }

    /**
     * POST the provided {@link CollectionDTO}-wrapped collection to {@link #batchUri}.
     * 
     * @param wrapped the collection to persist
     * @return the persistent collection
     */
    public CollectionModel<EntityModel<T>> saveAll(CollectionDTO<T> wrapped) {
        final ObjectNode node;
        final RestOperations restOperations = getRestOperations();
        try {
            node = restOperations.getRestTemplate()
                .postForObject(batchUri, wrapped, ObjectNode.class);
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return CollectionModel.wrap(Collections.<T>emptyList());
            }
            throw exception;
        }
        ObjectMapper objectMapper = restOperations.getObjectMapper();
        JavaType innerType = objectMapper.getTypeFactory()
            .constructParametricType(EntityModel.class, getEntityType());
        JavaType targetType = objectMapper.getTypeFactory()
            .constructParametricType(CollectionModel.class, innerType);

        CollectionModel<EntityModel<T>> result = objectMapper.convertValue(node, targetType);
        return result;
    }

    public static class Factory {
        private final Configuration configuration;
        private final ClientProxyFactory proxyFactory;
        private final RestOperations restOperations;
        private RestTemplateFactory restTemplateFactory = new DefaultRestTemplateFactory();
        private ObjectMapperFactory objectMapperFactory = new DefaultObjectMapperFactory();
        private final ClientProperties properties;

        public Factory(final ClientProperties properties) {
            this.properties = properties;
            this.configuration = uk.co.blackpepper.bowman.Configuration.builder()
                .setBaseUri(properties.getLocation())
                .build();
            this.proxyFactory = new JavassistClientProxyFactory();
            this.restOperations = new RestOperationsFactory(configuration, proxyFactory, objectMapperFactory, restTemplateFactory).create();
        }

        /**
         * Create a {@link CEClient} for the given annotated entity type.
         * 
         * @param <T> the entity type of the required client
         * @param entityType the entity type of the required client
         * @return the created client
         */
        public <T extends WithUri & WithId> CEClient<T> create(Class<T> entityType) {
            return new CEClient<>(entityType, properties.getBatchPath(), configuration, restOperations, proxyFactory);
        }
    }
}
