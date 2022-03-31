package org.datrunk.naked.demo;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datest.naked.test.entities.User;
import org.datrunk.naked.demo.repo.TutorialRepository;
import org.datrunk.naked.entities.config.CollectionDTOConverter;
import org.datrunk.naked.server.repo.BatchRestRepo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.ForwardedHeaderFilter;

@SpringBootApplication
@EnableConfigurationProperties({ Application.TomcatConnectionProperties.class })
@Import({ Application.Config.class })
public class Application extends SpringBootServletInitializer {
    private static Logger log = LogManager.getLogger();

    @SpringBootConfiguration
    @EntityScan(basePackageClasses = { User.class })
    @EnableJpaRepositories(basePackageClasses = { TutorialRepository.class }, considerNestedRepositories = true)
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
            });
        }

        @Bean
        public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
            FilterRegistrationBean<ForwardedHeaderFilter> result = new FilterRegistrationBean<>();
            result.setFilter(new ForwardedHeaderFilter());
            result.setOrder(0);
            return result;
        }
    }

    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication(Application.class);
        ConfigurableApplicationContext ctx = app.run(args);
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

    @Bean
    public ResourceConfig jerseyConfig() {
        return new JerseyConfig();
    }

    @ConfigurationProperties(prefix = "spring.datasource.tomcat")
    public static class TomcatConnectionProperties {
        private int validationQueryTimeout = 300;
        private int maxActive = 10;
        private int maxWait = 10;
        private int maxIdle = 10;

        public int getValidationQueryTimeout() {
            return validationQueryTimeout;
        }

        public void setValidationQueryTimeout(int validationQueryTimeout) {
            this.validationQueryTimeout = validationQueryTimeout;
        }

        public int getMaxActive() {
            return maxActive;
        }

        public void setMaxActive(int maxActive) {
            this.maxActive = maxActive;
        }

        public int getMaxWait() {
            return maxWait;
        }

        public void setMaxWait(int maxWait) {
            this.maxWait = maxWait;
        }

        public int getMaxIdle() {
            return maxIdle;
        }

        public void setMaxIdle(int maxIdle) {
            this.maxIdle = maxIdle;
        }
    }

    // @see {@link ApplicationContextProvider}
    @Bean
    public static ApplicationContextProvider contextProvider() {
        return new ApplicationContextProvider();
    }
}
