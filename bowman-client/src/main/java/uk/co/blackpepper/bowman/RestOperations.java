/*
 * Copyright 2016 Black Pepper Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.blackpepper.bowman;

import java.net.URI;
import java.util.Collections;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class RestOperations {

	private final RestTemplate restTemplate;
	
	private final ObjectMapper objectMapper;
	
	public RestOperations(RestTemplate restTemplate, ObjectMapper objectMapper) {
		this.restTemplate = restTemplate;
		this.objectMapper = objectMapper;
	}
	
	public <T> EntityModel<T> getResource(URI uri, Class<T> entityType) {
		ObjectNode node;
		
		try {
			node = restTemplate.getForObject(uri, ObjectNode.class);
		}
		catch (HttpClientErrorException exception) {
			if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
				return null;
			}
			
			throw exception;
		}
		
		JavaType targetType = objectMapper.getTypeFactory().constructParametricType(EntityModel.class, entityType);
		
		return objectMapper.convertValue(node, targetType);
	}

	public <T> CollectionModel<EntityModel<T>> getResources(URI uri, Class<T> entityType) {
		ObjectNode node;
		
		try {
			node = restTemplate.getForObject(uri, ObjectNode.class);
		}
		catch (HttpClientErrorException exception) {
			if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
				return CollectionModel.wrap(Collections.<T>emptyList());
			}
			
			throw exception;
		}
		
		JavaType innerType = objectMapper.getTypeFactory().constructParametricType(EntityModel.class, entityType);
		JavaType targetType = objectMapper.getTypeFactory().constructParametricType(CollectionModel.class, innerType);
		
		return objectMapper.convertValue(node, targetType);
	}
	
	public URI postForId(URI uri, Object object) {
		return restTemplate.postForLocation(uri, object);
	}
	
	public void put(URI uri, Object object) {
		restTemplate.put(uri, object);
	}
	
	public void delete(URI uri) {
		restTemplate.delete(uri);
	}
	
	public <T> EntityModel<T> patchForResource(URI uri, Object patch, Class<T> entityType) {
		ObjectNode node;

		node = restTemplate.patchForObject(uri, patch, ObjectNode.class);
		if (node == null) {
			return null;
		}

		JavaType targetType = objectMapper.getTypeFactory().constructParametricType(EntityModel.class, entityType);

		return objectMapper.convertValue(node, targetType);
	}

	public RestTemplate getRestTemplate() {
		return restTemplate;
	}
	
	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}
}
