package kohgylw.kiftd.server.configation;

import javax.sql.*;
import org.springframework.jdbc.datasource.*;
import kohgylw.kiftd.server.util.*;
import org.springframework.context.annotation.*;
import org.mybatis.spring.*;
import org.springframework.beans.factory.annotation.*;
import org.mybatis.spring.mapper.*;
import java.io.*;
import org.springframework.core.io.*;

@Configurable
public class DataAccess
{
    private static Resource[] mapperFiles;
    private static Resource mybatisConfg;
    
    @Bean
    public DataSource dataSource() {
        final DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl(ConfigureReader.instance().getFileNodePathURL());
        ds.setUsername("root");
        ds.setPassword("301537gY");
        return (DataSource)ds;
    }
    
    @Bean(name = { "sqlSessionFactory" })
    @Autowired
    public SqlSessionFactoryBean sqlSessionFactoryBean(final DataSource ds) {
        final SqlSessionFactoryBean ssf = new SqlSessionFactoryBean();
        ssf.setDataSource(ds);
        ssf.setConfigLocation(DataAccess.mybatisConfg);
        ssf.setMapperLocations(DataAccess.mapperFiles);
        return ssf;
    }
    
    @Bean
    public MapperScannerConfigurer mapperScannerConfigurer() {
        final MapperScannerConfigurer msf = new MapperScannerConfigurer();
        msf.setBasePackage("kohgylw.kiftd.server.mapper");
        msf.setSqlSessionFactoryBeanName("sqlSessionFactory");
        return msf;
    }
    
    static {
        final String mybatisResourceFolder = ConfigureReader.instance().getPath() + File.separator + "mybatisResource" + File.separator;
        final String mapperFilesFolder = mybatisResourceFolder + "mapperXML" + File.separator;
        DataAccess.mapperFiles = new Resource[] { new FileSystemResource(mapperFilesFolder + "NodeMapper.xml"), new FileSystemResource(mapperFilesFolder + "FolderMapper.xml") };
        DataAccess.mybatisConfg = (Resource)new FileSystemResource(mybatisResourceFolder + "mybatis.xml");
    }
}
