package work.cxlm.filecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;

/**
 * create 2021/4/1 21:53
 *
 * @author Chiru
 */
@SpringBootApplication(scanBasePackages = "work.cxlm.filecase")
@MapperScan(basePackages = "work.cxlm.filecase.dao.mapper")
@EnableCaching(proxyTargetClass = true)
@Slf4j
public class FileCaseApplication implements CommandLineRunner {
    public static void main(String[] args) {
        // 设置 druid 使用 slf4j 记录日志
        System.setProperty("druid.logType", "slf4j");
        SpringApplication.run(FileCaseApplication.class, args);
    }


    @Override
    public void run(String... args) {
        log.info("file-case 服务正在运行...");
    }
}
