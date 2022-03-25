package org.datrunk.naked.entities;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.apache.commons.lang3.builder.MyEqualsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableSet;

import uk.co.blackpepper.bowman.annotation.ResourceId;

/**
 * Unfortunately, some newly created {@link Entity} types include a database-managed identifier. These entities cannot
 * know their id value until after they are persisted. So, when created in code, their id is initialized to
 * <code>null</code>. Such entities should extend this class. If your {@link Entity}'s id is not generated within the
 * database, you should directly implement {@link WithId} instead of extending this class. If you want client support
 * and you don't extend this class, you should also implement {@link WithUri}.
 * <p>
 * This class overrides {@link #equals} and {@link #hashCode} so that {@link #getId} is a factor only if it is non-null.
 * When non-null, id is the only component in {@link #equals} and {@link #hashCode}. Conversely, when {@link #getId} is
 * null, it is excluded from {@link #equals} and {@link #hashCode}. Next, this class will consider {@link #getUri}. If
 * that is also null, then it is ignored and all other fields are included (via reflection).
 * 
 * @author ansonator
 *
 * @param <ID>
 */
public abstract class IdClass<ID> implements WithId<ID>, WithUri {
	@Transient
	@JsonIgnore
	transient private URI uri;

	@Override
	@ResourceId
	public URI getUri() {
		return uri;
	}

	@Override
	public void setUri(URI uri) {
		assert (this.uri == null || this.uri.equals(uri));
		this.uri = uri;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This override is useful when hashing an {@link Entity} with a database-generated {@link Id}. If {@link #getId}
	 * returns a non-null value, then only this value is included in the hash calculation. Otherwise, id is ignored and
	 * reflection is used to incorporate all other fields.
	 */
	@Override
	public int hashCode() {
		final ID id = getId();
		if (id != null) {
			return id.hashCode();
		}
		else if (getUri() != null) {
			return getUri().hashCode();
		}
		else {
			final int prime = 31;
			int result = super.hashCode();

			for (Method method : getClass().getDeclaredMethods()) {
				int modifiers = method.getModifiers();
				if (method.getName()
					.startsWith("get") && method.getParameterCount() == 0
					&& !method.getReturnType()
						.equals(Void.TYPE)
					&& !ImmutableSet.of("getId", "getUri", "getTypeId")
						.contains(method.getName())
					&& !Modifier.isTransient(modifiers)
					&& (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers))) {
					try {
						final Object val = method.invoke(this);
						if (val != null) {
							result = prime * result + val.hashCode();
						}
					}
					catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						throw new RuntimeException(e);
					}
				}
			}
			return result;
		}
	}

	@SuppressWarnings("unused")
	private static Stream<Method> getterStream(Class<?> clazz) {
		return Stream.of(clazz.getDeclaredMethods())
			.filter(method -> method.getName()
				.startsWith("get") && method.getParameterCount() == 0
				&& !method.getReturnType()
					.equals(Void.TYPE))
			.filter(method -> !ImmutableSet.of("getId", "getUri")
				.contains(method.getName()))
			.filter(method -> {
				int modifiers = method.getModifiers();
				return !Modifier.isTransient(modifiers)
					&& (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers));
			});
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This override is useful when comparing two {@link Entity}s with database-generated {@link Id}. If the argument is
	 * also an {@link IdClass} and both argument and receiver return non-null from {@link #getId}, then only their id
	 * values are compared. Otherwise, id is ignored and reflection is used to compare all other fields.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof IdClass<?>)) {
			return false;
		}
		final IdClass<?> other = (IdClass<?>) obj;
		@SuppressWarnings("unused")
		final IdClass<?> parent;
		if (getClass().isAssignableFrom(other.getClass())) {
			parent = this;
		}
		else if (other.getClass() 
			.isAssignableFrom(getClass())) {
			parent = other;
		}
		else {
			return false;
		}
		if (getId() != null && other.getId() != null) {
			return getId().equals(other.getId());
		}
		if (getUri() != null && other.getUri() != null) {
			return getUri().equals(other.getUri());
		}

		try {
			return MyEqualsBuilder.reflectionEquals(this, other, "getId", "getUri");
		}
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean reflectionEquals(IdClass<?> lhs, IdClass<?> rhs)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (lhs == rhs) {
			return true;
		}
		if (lhs == null || rhs == null) {
			return false;
		}
		final IdClass<?> parent;
		if (lhs.getClass()
			.isAssignableFrom(rhs.getClass())) {
			parent = lhs;
		}
		else if (rhs.getClass()
			.isAssignableFrom(lhs.getClass())) {
			parent = rhs;
		}
		else {
			return false;
		}
		for (Method method : parent.getClass()
			.getDeclaredMethods()) {
			if (method.getName()
				.startsWith("get") && method.getParameterCount() == 0 && method.getReturnType()
					.equals(Void.TYPE)) {
				method.invoke(lhs);
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return String.valueOf(uri);
	}

	public static boolean isId(Field field) {
		Set<Class<?>> annotations = Stream.of(field.getAnnotations())
			.map(Object::getClass)
			.filter(type -> ImmutableSet.of(Id.class, EmbeddedId.class)
				.contains(type))
			.collect(Collectors.toSet());
		return !annotations.isEmpty();
	}
}
