package work.cxlm.filecase.datasource.dynamic;

import java.util.Stack;

/**
 * 数据源的 Holder，允许嵌套数据源
 * create 2021/4/2 14:54
 *
 * @author Chiru
 */
public class DataSourceHolder {

    private static final ThreadLocal<Stack<String>> DATA_SOURCE_HOLDER = ThreadLocal.withInitial(Stack::new);

    /**
     * 设置数据源，在当前线程的 ThreadLocal 中 push 一个 datasource
     *
     * @param datasource 要使用的数据源名称
     */
    public static void setDataSource(String datasource) {
        DATA_SOURCE_HOLDER.get().push(datasource);
    }

    /**
     * 获取当前 ThreadLocal 中指定的 DataSource
     *
     * @return 应该使用的数据源名称，如果没有指定则使用默认值
     */
    public static String getDataSource() {
        if (!DATA_SOURCE_HOLDER.get().isEmpty()) {
            return DATA_SOURCE_HOLDER.get().peek();
        }
        return DS.DEFAULT_DATASOURCE_NAME;
    }

    /**
     * DataSource 注解作用域结束时调用
     */
    public static void clearDataSource() {
        if (!DATA_SOURCE_HOLDER.get().isEmpty()) {
            DATA_SOURCE_HOLDER.get().pop();

            if (DATA_SOURCE_HOLDER.get().isEmpty()) {
                DATA_SOURCE_HOLDER.remove();
            }
        }
    }
}
