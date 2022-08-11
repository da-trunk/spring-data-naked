package org.datrunk.naked.server.config;

import javax.persistence.EntityListeners;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import lombok.extern.log4j.Log4j2;

/**
 * Entity listener which allows dependency injection inside entities. The listener can be registered via {@link EntityListeners} annotation.
 * 
 * Dependency injection annotations like {@link Autowired} are supported.
 * 
 * <p>
 * Example:
 * 
 * <pre>
 * &#64;javax.persistence.Entity
 * &#64;javax.persistence.EntityListeners(SpringEntityListener.class)
 * public class Consumer {
 *   &#64;Autowired transient ConsumerRepo consumerRepo;
 *   
 *   ...
 * }
 * </pre>
 * 
 * For a more generally applicable method, see {@link ApplicationContextProvider}.
 * 
 * @author Christian Kaspari
 * @since 1.0.0
 * @see <a href=
 *      "https://github.com/CK35/example-ddd-with-spring-data-jpa/blob/master/src/main/java/de/ck35/example/ddd/jpa/SpringEntityListener.java">example-ddd-with-spring-data-jpa</a>
 * 
 */
@Log4j2
public class SpringEntityListener {
    private static final SpringEntityListener INSTANCE = new SpringEntityListener();

    private volatile AutowireCapableBeanFactory beanFactory;

    public static SpringEntityListener get() {
        return INSTANCE;
    }

    public AutowireCapableBeanFactory getBeanFactory() {
        return beanFactory;
    }

    public void setBeanFactory(AutowireCapableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @PostLoad
    @PostPersist
    public void inject(Object object) {
        AutowireCapableBeanFactory beanFactory = get().getBeanFactory();
        if (beanFactory == null) {
            log.warn("Bean Factory not set! Depdendencies will not be injected into: '{}'", object);
            return;
        }
        log.debug("Injecting dependencies into entity: '{}'.", object);
        beanFactory.autowireBean(object);
    }
}
