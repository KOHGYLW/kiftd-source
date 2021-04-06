package work.cxlm.filecase.datasource.dynamic;

import work.cxlm.filecase.datasource.util.DataSourceHelper;

import javax.sql.DataSource;

/**
 * 默认数据源工厂
 * create 2021/4/6 11:27
 *
 * @author Chiru
 */
public class DefaultDataSourceFactory extends BaseDataSourceFactory {

    private final String dataSourceKey;

    public DefaultDataSourceFactory(String dataSourceKey) {
        this.dataSourceKey = dataSourceKey;
    }

    @Override
    public DataSource getDataSource() {
        return applicationContext.getBean(DataSourceHelper.normalizeDataSourceName(dataSourceKey), DataSource.class);
    }
}
