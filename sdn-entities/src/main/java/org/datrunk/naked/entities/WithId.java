package org.datrunk.naked.entities;

/**
 * When an entity's id can be assigned by the code (a natural key), it should implement this interface. When the id is
 * generated within the database, the entity should instead extend {@link javax.persistence.IdClass}.
 * 
 * An entity implementing this should include a field {@code private ID id;} and class annotation
 * {@code @EqualsAndHashCode(of = { "id" })}.
 * 
 * @author da-trunk@outlook.com
 *
 * @param <ID>
 */
public interface WithId<ID> {
	ID getId();

	void setId(ID id);
}
