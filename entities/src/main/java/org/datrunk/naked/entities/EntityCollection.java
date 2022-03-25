package org.datrunk.naked.entities;

import java.io.Serializable;
import java.util.List;

public interface EntityCollection<T> extends Serializable {
	List<T> getEntities();

	void addEntity(T entity);
};
