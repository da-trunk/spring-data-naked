package org.datrunk.naked.server.config;

import javax.servlet.Servlet;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Opens a transaction early so that spring-data-rest won't create and close new transactions during a single request. Without this, it is
 * common for proxy field access to trigger a lazy load which fails because the Hibernate session managed by Spring's EntityManager has
 * already been closed. The session is closed when the transaction is closed.
 * 
 * Examples where not having this can result in failure:
 * 
 * <ul>
 * <li>Attempting to log an Entity may invoke a get request on a lazily loaded property. This triggers a new query in the DB which fails if
 * the transaction associated with the persistence context has already been closed.
 * <li>When an incoming POST or PATCH request payload includes links, spring-data-rest may open a new read only transaction and run queries
 * while deserializing. Spring may then inject an EntityManager with a closed transaction and session.
 * <li>When injecting an EntityManager into your custom controller, Spring may inject an EntityManager associated with a closed transaction
 * and Hibernate session. I think I've only seen this when logging in layers involved during JSON deserialization are enabled.
 * <li>When serializing the response from a POST or PATCH request, spring-data-rest will build links. This can trigger get requests on
 * proxies. During this, Hibernate may attempt to execute queries. I've seen this fail when our code has already committed the transaction.
 * Avoid this by ensuring any required getters are invoked before returning the EntityModel your custom controller method.
 * <li><tt>Example error: [org.springframework.http.converter.HttpMessageNotWritableException: Could not write JSON: could not initialize proxy [org.datrunk.naked.entities.User#role] - no Session; nested exception is com.fasterxml.jackson.databind.JsonMappingException: could not initialize proxy [org.datrunk.naked.entities.User#role] - no Session (through reference chain: org.springframework.hateoas.CollectionModel["_embedded"]->java.util.LinkedHashMap["users"]->java.util.ArrayList[0]->org.springframework.hateoas.EntityModel["content"]->org.datrunk.naked.entities.User["role"]->org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module$PersistentEntityResourceSerializer$1["content"]->org.datrunk.naked.entities.User$HibernateProxy$X8kcMzJw["name"])]</tt>
 * </ul>
 * 
 * <p>
 * There are other ways to get around this. See <a href="https://www.baeldung.com/java-jpa-lazy-collections">java-jpa-lazy-collections</a>
 * for a nice description of alternatives. Those approaches either have performance issues or require specific configuration for individual
 * use case. The approach taken here likely has drawbacks too. If we wrap to early, FrameworkServlet::processRequest will execute
 * publishRequestHandledEvent and we'll begin sending the response back to the client. This can cause us to return a successful (often
 * partial) response even though things will fail at commit on server.
 * 
 * In practice, things seem to work as long as the client and server are consistent regarding eager versus lazy loading. If the client
 * embeds an entity in the a POST or PATCH payload, then the server should eagerly fetch that entity. Both are controlled via annotations in
 * the entity, so make sure those annotations are consistent.
 * <p>
 * Inspired by <a href=
 * "https://stackoverflow.com/questions/25717127/handle-spring-data-rest-application-events-within-the-transaction">handle-spring-data-rest-application-events-within-the-transaction</a>
 * 
 * @author ansonator
 * @deprecated This uses Spring AOP, so it is limited to point cuts on Spring beans. In addition, advice will only be applied when the
 *             method is called from another class. Due to this, I was unable to find a good point cut. {@link Servlet#service} works as a
 *             point cut and it opens the transaction early enough to ensure the entire request runs under a single transaction. However, it
 *             opens it so early that SpringMVC begins sending the response back to the client before returning to the this advice and
 *             closing the transaction. That can cause the client to receive success response with a partial payload if anything fails when
 *             the transaction is later committed.
 *
 */
@Aspect
@Deprecated
public class SpringDataRestTransactionAspect {
    private TransactionTemplate transactionTemplate;

    public SpringDataRestTransactionAspect(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setName("around-data-rest-transaction");
        throw new RuntimeException(
            "This is broken.  It opens the transaction so early that it is committed after Spring has started sending a response back to the client");
    }

//    @Around("execution(* org.springframework.data.rest.webmvc.*Controller.*(..)) || execution(void org.springframework.web.servlet.DispatcherServlet.*(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse))")
//    @Around("execution(* org.springframework.data.rest.webmvc.*Controller.*(..)) || execution(void org.springframework.web.servlet.FrameworkServlet.service(javax.servlet.ServletRequest, javax.servlet.ServletResponse))")
//    @Around("execution(void org.apache.tomcat.websocket.server.WsFilter.doFilter(..))")
//    @Around("execution(void javax.servlet.Servlet.*(..))")
    @Around("execution(void javax.servlet.Servlet.service(javax.servlet.ServletRequest, javax.servlet.ServletResponse))")
//    @Around("execution(* org.springframework.data.repository.support.RepositoryInvoker.invokeSave(..))")
//    @Around("execution(* org.springframework.data.repository.CrudRepository.*(..))")
//    @Around("execution(* org.springframework.web.servlet.HandlerAdapter.handle(..))")
//    @Around("execution(* org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module$UriStringDeserializer.deserialize(..))")
//    @Around("execution(* com.fasterxml.jackson.databind.ObjectMapper.*(..))")
    public Object aroundDataRestCall(ProceedingJoinPoint joinPoint) {
        try {
            return transactionTemplate.execute(transactionStatus -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable e) {
                    transactionStatus.setRollbackOnly();
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (Exception ex) {
            throw ex;
        }
    }
}
