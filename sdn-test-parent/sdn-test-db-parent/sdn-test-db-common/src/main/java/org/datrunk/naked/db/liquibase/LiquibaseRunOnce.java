package org.datrunk.naked.db.liquibase;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import liquibase.integration.spring.SpringLiquibase;

/**
 * Disables Liquibase after first time it is invoked. Modifies existing SpringLiquibase autoconfigured bean instead of creating a new one.
 * 
 * If you want to run liquibase before every test class, comment this out.
 *
 * @author da-trunk@outlook.com
 *
 */
@Component
@ConditionalOnClass(SpringLiquibase.class)
@ConditionalOnProperty(prefix = "spring.liquibase", name = "enabled", matchIfMissing = false)
@AutoConfigureBefore({ SpringLiquibase.class })
public class LiquibaseRunOnce implements BeanPostProcessor {
    private static boolean hasRun = false;
    private SpringLiquibase liquibase = null;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!hasRun && (bean instanceof SpringLiquibase)) {
            liquibase = (SpringLiquibase) bean;
            liquibase.setShouldRun(false);
            hasRun = true;
        }
        return bean;
    }

    public void reset() {
        if (hasRun) {
            liquibase.setShouldRun(true);
            hasRun = false;
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
