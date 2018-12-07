package kohgylw.kiftd.server.configation;

import org.springframework.web.servlet.resource.*;

import com.google.gson.Gson;

import org.springframework.beans.factory.annotation.*;
import org.springframework.web.servlet.config.annotation.*;
import kohgylw.kiftd.server.util.*;
import java.io.*;
import javax.servlet.*;
import org.springframework.boot.web.servlet.*;
import org.springframework.context.annotation.*;

/**
 * 
 * <h2>Web功能-MVC相关配置类</h2>
 * <p>
 * 该Spring配置类主要负责配置kiftd网页服务器的处理行为。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Configurable
@ComponentScan({ "kohgylw.kiftd.server.controller", "kohgylw.kiftd.server.service.impl", "kohgylw.kiftd.server.util" })
@ServletComponentScan({ "kohgylw.kiftd.server.listener", "kohgylw.kiftd.server.filter" })
@Import({ DataAccess.class })
public class MVC extends ResourceHttpRequestHandler implements WebMvcConfigurer {
	
	@Override
	public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
		// TODO 自动生成的方法存根
		configurer.enable();
	}
	
	public void addResourceHandlers(final ResourceHandlerRegistry registry) {
		registry.addResourceHandler(new String[] { "/**" }).addResourceLocations(new String[] {
				"file:" + ConfigureReader.instance().getPath() + File.separator + "webContext" + File.separator });
		registry.addResourceHandler(new String[] { "/fileblocks/**" })
				.addResourceLocations(new String[] { "file:" + ConfigureReader.instance().getFileBlockPath() });
	}

	@Bean
	public MultipartConfigElement multipartConfigElement() {
		final MultipartConfigFactory factory = new MultipartConfigFactory();
		factory.setMaxFileSize(-1L);
		factory.setLocation(ConfigureReader.instance().getTemporaryfilePath());
		return factory.createMultipartConfig();
	}

	@Bean
	public Gson gson() {
		return new Gson();
	}
}
