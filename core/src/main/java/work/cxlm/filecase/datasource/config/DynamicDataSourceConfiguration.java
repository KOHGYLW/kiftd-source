package work.cxlm.filecase.datasource.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import work.cxlm.filecase.datasource.dynamic.DataSourceAdvisor;
import work.cxlm.filecase.datasource.dynamic.DataSourceInterceptor;
import work.cxlm.filecase.datasource.dynamic.DefaultDataSourceFactory;
import work.cxlm.filecase.datasource.dynamic.DynamicDataSourceFactory;
import work.cxlm.filecase.datasource.util.DataSourceHelper;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * create 2021/4/2 18:51
 *
 * @author Chiru
 */
@Slf4j
@Configuration
@AutoConfigureAfter(DataSourcePoolConfiguration.class)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@Import({DynamicDataSourceConfiguration.DruidDataSourceBeanPostProcessor.class,
        DynamicDataSourceConfiguration.DataSourceRegistrar.class})
public class DynamicDataSourceConfiguration {

    /**
     * 用于注册数据源
     */
    static class DataSourceRegistrar implements EnvironmentAware, BeanFactoryPostProcessor,
            Ordered {

        private Environment environment = null;

        private final AtomicBoolean registered = new AtomicBoolean(false);

        @Override
        public void postProcessBeanFactory(
                @NonNull ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
            if (configurableListableBeanFactory instanceof BeanDefinitionRegistry && !registered.get()) {
                registerBeanDefinitions((BeanDefinitionRegistry) configurableListableBeanFactory);
                registered.set(true);
            }
        }

        @Override
        public void setEnvironment(@NonNull Environment environment) {
            this.environment = environment;
        }

        private void registerBeanDefinitions(BeanDefinitionRegistry registry) {
            // 读取各项配置
            Boolean autoSetPrimary = environment.getProperty("datasource.auto-set-primary", Boolean.class, Boolean.TRUE);
            Boolean dynamicDataSource = environment.getProperty("datasource.use-dynamic", Boolean.class, Boolean.FALSE);
            // 主数据源
            String primaryDataSource = environment.getProperty("datasource.primary", String.class);

            Map<String, Boolean> dependencyRoots = Maps.newHashMap();
            Map<String, Object> dataSourcePools = DataSourceHelper.getDataSourcePoolBindings(environment);
            dataSourcePools.keySet().forEach(key -> {
                val dsName = DataSourceHelper.normalizeDataSourceName(key);
                val builder = BeanDefinitionBuilder
                        .genericBeanDefinition(DruidDataSource.class)
                        .setInitMethodName("init")
                        .setDestroyMethodName("close");

                AbstractBeanDefinition bd = builder.getBeanDefinition();

                // 默认是根
                dependencyRoots.put(key, Boolean.TRUE);
                if (Boolean.TRUE.equals(autoSetPrimary) && key.equals(primaryDataSource)) {
                    bd.setPrimary(true);
                }

                registry.registerBeanDefinition(dsName, bd);
                log.info("注册数据源: {}", dsName);
            });

            if (registry.containsBeanDefinition(DataSourceConst.DATA_SOURCE_CAMEL)) {
                /*
                 * org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration 会自动初始化。
                 * 顺序在此之前，这会优先将 Hikari 注册为数据源，导致数据源无法加载。
                 *
                 * 为不影响 Bean 的加载顺序，以方便后续可能的拓展（比如引入外部配置中心）
                 * 这里直接把 Hikari 的 BeanDefinition 删除。
                 */
                BeanDefinition bd = registry.getBeanDefinition(DataSourceConst.DATA_SOURCE_CAMEL);
                if (bd.toString().contains(DataSourceConst.HIKARI_NAME)) {
                    registry.removeBeanDefinition(DataSourceConst.DATA_SOURCE_CAMEL);
                    String scopedTargetDatasourceBeanName = "scopedTarget.dataSource";
                    if (registry.containsBeanDefinition(scopedTargetDatasourceBeanName)) {
                        registry.removeBeanDefinition(scopedTargetDatasourceBeanName);
                    }
                }
            }
            // 如果使用动态数据源配置
            if (dynamicDataSource) {
                // 使用动态数据源时，需要配置默认数据源
                String defaultDataSource = environment.getProperty("datasource.dynamic-default", String.class);
                Preconditions.checkState(!StringUtils.isEmpty(defaultDataSource),
                        "使用动态数据源时，必须配置 'datasource.dynamic-default' 参数!!!");
                registerDataSource(registry, defaultDataSource, DynamicDataSourceFactory.class, dependencyRoots, defaultDataSource);
            } else if (dependencyRoots.size() == 1) {
                // 如果只有一个，则默认为DataSourceConst.DATA_SOURCE_SUFFIX
                if (!registry.containsBeanDefinition(DataSourceConst.DATA_SOURCE_SUFFIX)) {
                    // 读取 Map 并根据键名集合注册 Bean
                    for (Map.Entry<String, Boolean> entry : dependencyRoots.entrySet()) {
                        registerDataSource(registry, entry.getKey(), DefaultDataSourceFactory.class, dependencyRoots, entry.getKey());
                    }
                }
            } else if (!StringUtils.isEmpty(primaryDataSource)) {
                // 设定了 primary 数据源的情况
                registerDataSource(registry, primaryDataSource, DefaultDataSourceFactory.class, dependencyRoots, primaryDataSource);
            }
        }

        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }

        private void registerDataSource(BeanDefinitionRegistry registry, String dataSourceName, Class<?> factoryClass, Object ...constructorArgValue) {
            // 根据数据源名称注册数据源
            String dataSourceFactoryBeanName = dataSourceName + factoryClass.getSimpleName();
            BeanDefinitionBuilder factoryBuilder = BeanDefinitionBuilder.genericBeanDefinition(factoryClass);
            for (Object arg : constructorArgValue) {
                factoryBuilder.addConstructorArgValue(arg);
            }
            registry.registerBeanDefinition(dataSourceFactoryBeanName, factoryBuilder.getBeanDefinition());

            BeanDefinition bd = new GenericBeanDefinition();
            bd.setFactoryBeanName(dataSourceFactoryBeanName);
            bd.setFactoryMethodName("getDataSource");
            registry.registerBeanDefinition(DataSourceConst.DATA_SOURCE_CAMEL, bd);
        }
    }

    /**
     * DruidDataSource 的 Bean 处理器，将各数据源的自定义配置绑定到 Bean
     * 即读取配置文件中 datasource.pool 中的配置，绑定到 Map 中
     */
    static class DruidDataSourceBeanPostProcessor implements EnvironmentAware, BeanPostProcessor,
            Ordered {

        private Environment environment;

        @Override
        public void setEnvironment(@NonNull Environment environment) {
            // 通过本方法可以拿到配置文件中的属性
            this.environment = environment;
        }

        @Override
        public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName)
                throws BeansException {
            if (bean instanceof DruidDataSource) {
                // 设置 Druid 名称
                val ds = (DruidDataSource) bean;
                ds.setName(beanName);
                // 获取配置文件中配置的参数并绑定到指定的 Map
                Map<String, Object> dataSources = DataSourceHelper.getDataSourcePoolBindings(environment);
                // 存在合法配置时，将配置绑定到当前的 bean，即 DataSource 实例上
                if (!dataSources.isEmpty() && dataSources.containsKey(DataSourceHelper.parseDataSourceKey(beanName))) {
                    Binder.get(environment).bind(DataSourceConst.DATA_SOURCE_POOL_PREFIX + "." +
                            DataSourceHelper.parseDataSourceKey(beanName), Bindable.ofInstance(ds));
                }
                log.info("解析数据源配置: {}, url={}", beanName, ds.getUrl());
            }
            return bean;
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }

    @Bean
    @ConditionalOnProperty(name = "datasource.aspect.routing.enable", havingValue = "true", matchIfMissing = true)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public DataSourceAdvisor dynamicDatasourceAnnotationAdvisor() {
        DataSourceInterceptor interceptor = new DataSourceInterceptor();
        return new DataSourceAdvisor(interceptor);
    }

    @Bean
    @ConditionalOnProperty(name = "datasource.override.initializer.enable", havingValue = "true", matchIfMissing = true)
    public DataSourceInitializerPostProcessor dataSourceInitializerPostProcessor() {
        return new DataSourceInitializerPostProcessor();
    }

    static class DataSourceInitializerPostProcessor implements BeanPostProcessor, Ordered {

        DataSourceInitializerPostProcessor() {
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }

        @Override
        public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
            return bean;
        }
    }
}
