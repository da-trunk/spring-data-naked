package org.datrunk.naked.server.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Enables injection into classes not managed by spring. For example:
 * 
 * <pre>
 * &#64;Entity
 * public class Account {
 *     public void doAccountRepositoryStuff() {
 *         AccountRepository accountRepository = (AccountRepository) Application.contextProvider()
 *             .getApplicationContext()
 *             .getBean("accountRepository");
 *     }
 * }
 * </pre>
 * 
 * @author pleft
 * @see <a href=
 *      "https://stackoverflow.com/questions/46092710/how-can-i-access-the-repository-from-the-entity-in-spring-boot">how-can-i-access-the-repository-from-the-entity-in-spring-boo</a>
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {
    private static ApplicationContext context;

    public ApplicationContext getApplicationContext() {
        return context;
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        context = ctx;
    }
}
