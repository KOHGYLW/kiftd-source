package work.cxlm.filecase.datasource.util;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import work.cxlm.filecase.datasource.config.DataSourceConst;

import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * 处理数据源注入的静态工具类
 * create 2021/4/5 23:41
 *
 * @author Chiru
 */
public class DataSourceHelper {

    private static ApplicationContext applicationContext = null;

    /**
     * 根据配置文件获取绑定的 Map
     *
     * @param environment 配置文件中读到的配置项
     * @return 绑定获得的 Map
     */
    public static Map<String, Object> getDataSourcePoolBindings(Environment environment) {
        return Binder.get(environment)
                .bind(DataSourceConst.DATA_SOURCE_POOL_PREFIX,
                        Bindable.mapOf(String.class, Object.class))
                .orElse(emptyMap());
    }

    /**
     * 去掉数据源 DataSource 后缀
     *
     * @param dsName 数据源原名
     * @return 如果 dsName 以 DataSource 结尾则去掉，否则不作处理
     */
    public static String parseDataSourceKey(String dsName) {
        if (dsName.endsWith(DataSourceConst.DATA_SOURCE_SUFFIX)) {
            return dsName.substring(0, dsName.indexOf(DataSourceConst.DATA_SOURCE_SUFFIX));
        }
        return dsName;
    }

    /**
     * 为数据源名称补充 DataSource 后缀名
     *
     * @param name 原数据源名称
     * @return 如果 name 不包含 DataSource 后缀则添加，否则不作处理
     */
    public static String normalizeDataSourceName(String name) {
        if (StringUtils.isEmpty(name) || DataSourceConst.DATA_SOURCE_CAMEL.equals(name)) {
            return DataSourceConst.DATA_SOURCE_CAMEL;
        }
        if (name.endsWith(DataSourceConst.DATA_SOURCE_SUFFIX)) {
            return name;
        }
        return name + DataSourceConst.DATA_SOURCE_SUFFIX;
    }

    public static void setApplicationContext(ApplicationContext applicationContext) {
        DataSourceHelper.applicationContext = applicationContext;
    }

    /**
     * 获取 context 中配置的多数据源默认配置
     *
     * @return 使用多数据源时的默认数据源
     */
    public static String getDynamicDefaultDataSource() {
        if (applicationContext == null) {
            throw new NullPointerException("ApplicationContext 象为 null");
        }
        return applicationContext.getEnvironment().getProperty("datasource.dynamic-default");
    }
}
