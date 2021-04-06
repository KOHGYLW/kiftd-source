package work.cxlm.filecase.datasource.dynamic;

import java.lang.annotation.*;

/**
 * 动态数据源注解
 * create 2021/4/2 12:23
 *
 * @author Chiru
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DS {

    String DEFAULT_DATASOURCE_NAME = "default";

    /**
     * 绑定数据源
     */
    String dataSource() default DEFAULT_DATASOURCE_NAME;
}
