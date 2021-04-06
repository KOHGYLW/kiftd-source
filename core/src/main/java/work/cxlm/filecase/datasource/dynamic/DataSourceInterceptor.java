package work.cxlm.filecase.datasource.dynamic;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;
import work.cxlm.filecase.datasource.util.DataSourceHelper;

/**
 * DataSource 注解拦截器
 * create 2021/4/2 14:14
 *
 * @author Chiru
 */
public class DataSourceInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        DS annotationOnMethod = AnnotationUtils.findAnnotation(methodInvocation.getMethod(), DS.class);
        String dataSourceName;
        if (annotationOnMethod != null && !StringUtils.isEmpty(annotationOnMethod.dataSource())) {
            dataSourceName = annotationOnMethod.dataSource();
        } else {
            dataSourceName = DS.DEFAULT_DATASOURCE_NAME;
        }
        try {
            if (!StringUtils.isEmpty(dataSourceName)) {
                DataSourceHolder.setDataSource(dataSourceName);
            } else {
                DataSourceHolder.setDataSource(DataSourceHelper.getDynamicDefaultDataSource());
            }
            return methodInvocation.proceed();
        } finally {
            DataSourceHolder.clearDataSource();
        }
    }
}
