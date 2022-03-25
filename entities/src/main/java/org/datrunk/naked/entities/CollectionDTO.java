package org.datrunk.naked.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Transient;

import org.datrunk.naked.entities.config.EntityIdResolver;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

public class CollectionDTO<T extends WithId> implements EntityCollection<T> {
	private static final long serialVersionUID = 1L;

	@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
	@JsonTypeIdResolver(value = EntityIdResolver.class)
	private List<T> entities = new ArrayList<>();

	@JsonTypeId
	@Transient
	private String typeId;

	@JsonCreator
	public CollectionDTO(@JsonProperty("entities") List<T> entities) {
		this.entities = entities;
	}

	@Override
	public String toString() {
		return entities.toString();
	}

	@Override
	public List<T> getEntities() {
		return entities;
	}

	protected void setEntities(List<T> entities) {
		this.entities = entities;
	}

	public static <T extends WithId> CollectionDTO<T> create() {
		return new CollectionDTO<>(new ArrayList<>());
	}

	public static <T extends WithId> CollectionDTO<T> create(Collection<T> entities) {
		return new CollectionDTO<>(new ArrayList<>(entities));
	}

	@Override
	public void addEntity(T entity) {
		add(entity);
	}

	public boolean add(T entity) {
		this.entities.add(entity);
		return true;
	}

	public boolean addAll(Collection<? extends T> entities) {
		this.entities.addAll(entities);
		return true;
	}

	public void clear() {
		this.entities.clear();
	}

	public int size() {
		return this.entities.size();
	}

	public Iterator<T> iterator() {
		return this.entities.iterator();
	}
};
