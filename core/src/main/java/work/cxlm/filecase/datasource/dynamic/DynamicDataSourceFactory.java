package work.cxlm.filecase.datasource.dynamic;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import work.cxlm.filecase.datasource.util.DataSourceHelper;

import javax.sql.DataSource;
import java.util.Map;

/**
 * 动态数据源工厂
 * create 2021/4/2 15:27
 *
 * @author Chiru
 */
@Setter
@Getter
public class DynamicDataSourceFactory extends BaseDataSourceFactory {

    private Map<String, Boolean> dataSourceNames;

    private String defaultDataSourceName;

    public DynamicDataSourceFactory(Map<String, Boolean> dataSourceNames, String defaultDataSourceName) {
        this.dataSourceNames = dataSourceNames;
        this.defaultDataSourceName = defaultDataSourceName;
    }

    @Override
    public DataSource getDataSource() {
        DataSource defaultDataSource = null;
        Map<Object, Object> targetDataSource = Maps.newHashMap();
        for (Map.Entry<String, Boolean> entry : dataSourceNames.entrySet()) {
            DataSource dataSource = applicationContext.getBean(DataSourceHelper.normalizeDataSourceName(entry.getKey()), DataSource.class);
            DataSourceHelper.setApplicationContext(applicationContext);
            targetDataSource.put(entry.getKey(), dataSource);
            // 如果没有设置默认数据源则使用第一个，否则使用设置的
            if (defaultDataSource == null || entry.getKey().equals(defaultDataSourceName)) {
                defaultDataSource = dataSource;
            }
        }
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        dynamicDataSource.setTargetDataSources(targetDataSource);
        if (defaultDataSource == null) {
            dynamicDataSource.setDefaultTargetDataSource(DS.DEFAULT_DATASOURCE_NAME);
        } else {
            dynamicDataSource.setDefaultTargetDataSource(defaultDataSource);
        }
        return dynamicDataSource;
    }

}
