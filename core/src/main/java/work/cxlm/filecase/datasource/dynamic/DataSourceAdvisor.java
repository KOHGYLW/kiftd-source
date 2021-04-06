package work.cxlm.filecase.datasource.dynamic;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.lang.NonNull;

/**
 * DataSource 注解切面
 * create 2021/4/2 12:33
 *
 * @author Chiru
 */
public class DataSourceAdvisor extends AbstractPointcutAdvisor implements
        BeanFactoryAware {

    private final Pointcut cachedPointcut;
    private final Advice cachedAdvice;

    public DataSourceAdvisor(DataSourceInterceptor dataSourceInterceptor) {
        cachedAdvice = dataSourceInterceptor;
        Pointcut cpc = new AnnotationMatchingPointcut(DS.class, true);
        Pointcut mpc = AnnotationMatchingPointcut.forMethodAnnotation(DS.class);
        cachedPointcut = new ComposablePointcut(cpc).union(mpc);
    }

    @Override
    @NonNull
    public Pointcut getPointcut() {
        return cachedPointcut;
    }

    @Override
    @NonNull
    public Advice getAdvice() {
        return cachedAdvice;
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        if (cachedAdvice instanceof BeanFactoryAware) {
            ((BeanFactoryAware) cachedAdvice).setBeanFactory(beanFactory);
        }
    }
}
