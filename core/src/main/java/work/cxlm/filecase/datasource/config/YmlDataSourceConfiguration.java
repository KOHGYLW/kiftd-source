package work.cxlm.filecase.datasource.config;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * 接收多数据源配置
 * create 2021/4/2 12:00
 *
 * @author Chiru
 */
@Data
@ConfigurationProperties("datasource")
@Configuration
public class YmlDataSourceConfiguration {

    private HashMap<String, DruidDataSource> pool = new HashMap<>();

}
