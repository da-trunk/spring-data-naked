package org.datrunk.naked.server.config;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datest.naked.test.entities.User;
import org.datrunk.naked.entities.config.CollectionDTOConverter;
import org.datrunk.naked.server.entities.AppDetails;
import org.datrunk.naked.server.repo.BaseRepositoryImpl;
import org.datrunk.naked.server.repo.BatchRestRepo;
import org.datrunk.naked.server.repo.UserRepo;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.ForwardedHeaderFilter;

@SpringBootApplication
// @Configuration
// @EnableAutoConfiguration
@EnableConfigurationProperties({ AppDetails.class, TomcatConnectionProperties.class })
// @ComponentScan({})
@Import({ Application.Config.class })
@PropertySources({ @PropertySource("classpath:application-server.yml"), @PropertySource("classpath:application-default.yml"),
    @PropertySource("classpath:application-h2.yml") })
public class Application extends SpringBootServletInitializer {
    private static Logger log = LogManager.getLogger();

    @SpringBootConfiguration
    @EntityScan(basePackageClasses = { User.class })
    @EnableJpaRepositories(basePackageClasses = { UserRepo.class }, repositoryBaseClass = BaseRepositoryImpl.class,
        considerNestedRepositories = true)
    @ComponentScan(basePackageClasses = { BatchRestRepo.class, CollectionDTOConverter.class })
    @EnableHypermediaSupport(type = HypermediaType.HAL)
    @EnableAspectJAutoProxy
    @EnableTransactionManagement
    @EnableConfigurationProperties
    public static class Config {
        @Bean
        public RepositoryRestConfigurer repositoryRestConfigurer(EntityManager entityManager) {
            return RepositoryRestConfigurer.withConfig(config -> {
                config.exposeIdsFor(entityManager.getMetamodel()
                    .getEntities()
                    .stream()
                    .map(javax.persistence.metamodel.Type::getJavaType)
                    .toArray(Class[]::new));
                // assert(config.getBasePath().equals(URI.create("/api")));
            });
        }

        // @Bean
        // public SpringDataRestTransactionAspect springDataRestTransactionAspect(PlatformTransactionManager transactionManager) {
        // return new SpringDataRestTransactionAspect(transactionManager);
        // }

        // see https://tech.asimio.net/2020/04/06/Adding-HAL-Hypermedia-to-Spring-Boot-2-applications-using-Spring-HATEOAS.html
        @Bean
        public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
            FilterRegistrationBean<ForwardedHeaderFilter> result = new FilterRegistrationBean<>();
            result.setFilter(new ForwardedHeaderFilter());
            result.setOrder(0);
            return result;
        }
    }

    // Bootstrap the application
    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication(Application.class);
        ConfigurableApplicationContext ctx = app.run(args);
        // RepositoryRestConfiguration restConfiguration = ctx.getBean(RepositoryRestConfiguration.class);
        // log.info("spring.data.rest.return-body-on-create=[{}]", restConfiguration.isReturnBodyOnCreate());
        // log.info("spring.data.rest.return-body-on-update=[{}]", restConfiguration.isReturnBodyOnUpdate());
        // restConfiguration.setReturnBodyForPutAndPost(true);
    }

    @RestController
    public static class TestController {
        @GetMapping("/test/controller")
        public String handler() {
            return "success!";
        }
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

    @Bean
    public FilterRegistrationBean<ForwardedHeaderFilter> loggingFilter() {
        FilterRegistrationBean<ForwardedHeaderFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new ForwardedHeaderFilter());
        return registrationBean;
    }

    @Primary
    @Bean(destroyMethod = "close")
    public DataSource primaryDataSource(DataSourceProperties properties, TomcatConnectionProperties tomcatProperties) {
        log.info("Connecting to [{}] at [{}]", properties.getUsername(), properties.getUrl());

        @SuppressWarnings("cast")
        org.apache.tomcat.jdbc.pool.DataSource dataSource =
            (org.apache.tomcat.jdbc.pool.DataSource) properties.initializeDataSourceBuilder()
                .type(org.apache.tomcat.jdbc.pool.DataSource.class)
                .build();

        DatabaseDriver databaseDriver = DatabaseDriver.fromJdbcUrl(properties.determineUrl());
        String validationQuery = databaseDriver.getValidationQuery();
        if (validationQuery != null) {
            dataSource.setTestOnBorrow(true);
            dataSource.setValidationQuery(validationQuery);
            dataSource.setValidationQueryTimeout(tomcatProperties.getValidationQueryTimeout());
        }
        dataSource.setMaxActive(tomcatProperties.getMaxActive());
        dataSource.setMaxWait(tomcatProperties.getMaxWait());
        dataSource.setMaxIdle(tomcatProperties.getMaxIdle());
        return dataSource;
    }

    /*
     * @Bean(destroyMethod = "close")
     * 
     * @SecondaryDataSource public DataSource secondaryDataSource(DataSourceProperties properties, TomcatConnectionProperties
     * tomcatProperties,
     * 
     * @Value("${spring.datasource.secondary.username}") String userName, @Value("${spring.datasource.secondary.password}") String password)
     * { String url = properties.getUrl() .replace(properties.getUsername(), userName) .replace(properties.getPassword(), password);
     * log.info("Connecting to [{}] at [{}]", userName, url);
     * 
     * @SuppressWarnings("cast") org.apache.tomcat.jdbc.pool.DataSource dataSource = (org.apache.tomcat.jdbc.pool.DataSource)
     * properties.initializeDataSourceBuilder() .type(org.apache.tomcat.jdbc.pool.DataSource.class) .url(url) .username(userName)
     * .password(password) .build();
     * 
     * DatabaseDriver databaseDriver = DatabaseDriver.fromJdbcUrl(properties.determineUrl()); String validationQuery =
     * databaseDriver.getValidationQuery(); if (validationQuery != null) { dataSource.setTestOnBorrow(true);
     * dataSource.setValidationQuery(validationQuery); dataSource.setValidationQueryTimeout(tomcatProperties.getValidationQueryTimeout()); }
     * dataSource.setMaxActive(tomcatProperties.getMaxActive()); dataSource.setMaxWait(tomcatProperties.getMaxWait());
     * dataSource.setMaxIdle(tomcatProperties.getMaxIdle()); return dataSource; }
     */

    @Bean
    public ResourceConfig jerseyConfig() {
        return new JerseyConfig();
    }

    // @see {@link ApplicationContextProvider}
    @Bean
    public static ApplicationContextProvider contextProvider() {
        return new ApplicationContextProvider();
    }
}
