package work.cxlm.filecase.datasource.dynamic;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;

import javax.sql.DataSource;

/**
 * create 2021/4/6 11:29
 *
 * @author Chiru
 */
public abstract class BaseDataSourceFactory implements ApplicationContextAware, EnvironmentAware {

    protected ApplicationContext applicationContext;

    protected Environment environment;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    /**
     * 获取数据源 DataSource 实例
     *
     * @return DataSource 实例
     */
    public abstract DataSource getDataSource();
}